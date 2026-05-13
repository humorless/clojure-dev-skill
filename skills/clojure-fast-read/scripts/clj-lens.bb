#!/usr/bin/env bb
;; clj-lens: Multi-mode Structural Code Reader
(require '[rewrite-clj.zip :as z]
         '[cheshire.core :as json]
         '[clojure.string :as str]
         '[clojure.java.shell :as shell])

;; ============================================================================
;; Output Envelope
;; ============================================================================

(defn ok-response [mode data]
  {:status "ok" :mode mode :data data})

(defn error-response [error-type message & details]
  (let [base {:status "error" :error error-type :message message}]
    (if (seq details)
      (assoc base :details (first details))
      base)))

(defn suggestion-response [message matches]
  {:status "suggestion" :message message :matches matches})

(defn print-json [response]
  (println (json/generate-string response)))

;; ============================================================================
;; clj-kondo Integration
;; ============================================================================

(defn clj-kondo-available? []
  "Check if clj-kondo is available on PATH"
  (let [result (try
                 (shell/sh "clj-kondo" "--version" {:out :string :err :string})
                 (catch Exception _ nil))]
    (and result (zero? (:exit result)))))

(defn query-clj-kondo []
  "Run clj-kondo export and return parsed JSON"
  (try
    (let [result (shell/sh "clj-kondo" "export" "--format" "json"
                          {:out :string :err :string})]
      (if (zero? (:exit result))
        (json/parse-string (:out result) true)
        nil))
    (catch Exception _ nil)))

(defn find-by-symbol [analysis symbol-name]
  "Search clj-kondo analysis for exact symbol match"
  (let [var-defs (or (get analysis :var-definitions) [])
        parts (str/split symbol-name #"/")
        ns-name (first parts)
        var-name (second parts)]
    (if var-name
      (filter (fn [def]
                (and (= (:namespace def) ns-name)
                     (= (:name def) var-name)))
              var-defs)
      (filter (fn [def]
                (= (:name def) ns-name))
              var-defs))))

(defn find-by-pattern [analysis pattern]
  "Search clj-kondo analysis for symbols matching pattern (substring)"
  (let [var-defs (or (get analysis :var-definitions) [])]
    (filter (fn [def]
              (str/includes? (:name def) pattern))
            var-defs)))

;; ============================================================================
;; nREPL Integration
;; ============================================================================

(defn find-nrepl-port []
  "Find nREPL port from .nrepl-port file"
  (let [port-file ".nrepl-port"]
    (if (.exists (java.io.File. port-file))
      (str/trim (slurp port-file))
      nil)))

(defn query-nrepl [code]
  "Execute code in nREPL and return result"
  (let [port (find-nrepl-port)]
    (if-not port
      nil
      (try
        (let [result (shell/sh "brepl" "-c" code {:out :string :err :string})]
          (if (zero? (:exit result))
            (:out result)
            nil))
        (catch Exception _ nil)))))

(defn get-last-exception []
  "Get the last exception from nREPL (*e)"
  (query-nrepl "(when-let [e *e] (.toString e))"))

;; ============================================================================
;; Mode: Coordinate (Existing Behavior, JSON-wrapped)
;; ============================================================================

(defn coordinate-mode [file line]
  (try
    (let [zloc (z/of-file file)
          row (Integer/parseInt line)
          match (z/find-depth-first zloc #(= (-> % z/node meta :row) row))]
      (if match
        (print-json (ok-response "coordinate"
                                 {:file file
                                  :line (Integer/parseInt line)
                                  :form (z/string match)}))
        (do (print-json (error-response "not-found"
                                       (str "No form found at line " line)))
            (System/exit 1))))
    (catch Exception e
      (print-json (error-response "read-error" (.getMessage e)))
      (System/exit 1))))

;; ============================================================================
;; Mode: Symbol Lookup (Implemented in Task 3)
;; ============================================================================

(defn symbol-mode [symbol-name]
  (let [analysis (query-clj-kondo)]
    (if-not analysis
      (do (print-json (error-response "clj-kondo-unavailable"
                                     "clj-kondo not found. Install with: npm install -g clj-kondo"))
          (System/exit 1))
      (let [matches (find-by-symbol analysis symbol-name)]
        (if (empty? matches)
          ;; No exact match, try suggestions
          (let [suggestions (find-by-pattern analysis
                                              (last (str/split symbol-name #"/")))]
            (if (empty? suggestions)
              (do (print-json (error-response "symbol-not-found"
                                             (str "Symbol " symbol-name " not found")))
                  (System/exit 1))
              (print-json (suggestion-response
                          "Exact match not found. Did you mean one of these?"
                          (map #(select-keys % [:name :namespace :file :line]) suggestions)))))
          ;; Exact match found, extract code
          (let [match (first matches)
                file (:file match)
                line (:line match)]
            (try
              (let [zloc (z/of-file file)
                    row line
                    form-match (z/find-depth-first zloc #(= (-> % z/node meta :row) row))]
                (if form-match
                  (print-json (ok-response "symbol"
                                          {:symbol symbol-name
                                           :file file
                                           :line line
                                           :form (z/string form-match)}))
                  (do (print-json (error-response "form-not-extracted"
                                                (str "Could not extract form at line " line)))
                      (System/exit 1))))
              (catch Exception e
                (do (print-json (error-response "read-error" (.getMessage e)))
                    (System/exit 1))))))))))

;; ============================================================================
;; Mode: Find (Task 4)
;; ============================================================================

(defn find-mode [pattern]
  (let [analysis (query-clj-kondo)]
    (if-not analysis
      (do (print-json (error-response "clj-kondo-unavailable"
                                     "clj-kondo not found. Install with: npm install -g clj-kondo"))
          (System/exit 1))
      (let [matches (find-by-pattern analysis pattern)]
        (if (empty? matches)
          (print-json (ok-response "find"
                                  {:pattern pattern
                                   :matches []}))
          (print-json (ok-response "find"
                                  {:pattern pattern
                                   :matches (map #(select-keys % [:name :namespace :file :line])
                                                matches)})))))))

;; ============================================================================
;; Mode: Last Error (Task 6)
;; ============================================================================

(defn parse-exception-location [exception-str]
  "Extract file and line from a Clojure/Java exception"
  (let [matches (re-find #"(\S+\.clj):(\d+)" exception-str)]
    (if matches
      {:file (second matches) :line (Integer/parseInt (nth matches 2))}
      nil)))

(defn analyze-error-context [form]
  "Simple heuristic analysis of error context"
  {:suspect "unknown" :reason "manual-inspection-needed"})

(defn last-error-mode []
  (try
    (let [port (find-nrepl-port)]
      (if-not port
        (do (print-json (error-response "nrepl-unavailable"
                                       "nREPL server not found. Start with: clj -M:nrepl or bb nrepl-server 1667"))
            (System/exit 1))
        (let [exc-str (get-last-exception)]
          (if-not exc-str
            (do (print-json (error-response "no-exception" "No exception in *e"))
                (System/exit 1))
            (let [location (parse-exception-location exc-str)]
              (if-not location
                (do (print-json (error-response "parse-failed"
                                               (str "Could not parse exception location: " exc-str)))
                    (System/exit 1))
                (let [file (:file location)
                      line (:line location)]
                  (try
                    (let [zloc (z/of-file file)
                          form-match (z/find-depth-first zloc
                                                        #(= (-> % z/node meta :row) line))]
                      (if form-match
                        (print-json (ok-response "last-error"
                                               {:error (subs exc-str 0 (min 50 (count exc-str)))
                                                :location location
                                                :form (z/string form-match)
                                                :analysis (analyze-error-context (z/string form-match))}))
                        (do (print-json (error-response "form-not-found"
                                                      (str "Could not extract form at line " line)))
                            (System/exit 1))))
                    (catch Exception e
                      (do (print-json (error-response "read-error" (.getMessage e)))
                          (System/exit 1)))))))))))
    (catch Exception e
      (do (print-json (error-response "nrepl-error" (.getMessage e)))
          (System/exit 1)))))

;; ============================================================================
;; Mode: Trace (Task 7)
;; ============================================================================

(defn parse-stacktrace [stacktrace-str]
  "Parse a Clojure/Java stacktrace into frames with file/line info"
  (let [lines (str/split stacktrace-str #"\n")
        pattern #"at\s+([\w\.]+)\s*\(([^:]+):(\d+)\)"]
    (keep (fn [line]
            (let [matches (re-find pattern line)]
              (when matches
                {:class (second matches)
                 :method ""
                 :file (nth matches 2)
                 :line (Integer/parseInt (nth matches 3))})))
          lines)))

(defn trace-mode [stacktrace]
  (try
    (let [frames (parse-stacktrace stacktrace)]
      (if (empty? frames)
        (do (print-json (error-response "parse-failed"
                                       "Could not parse any frames from stacktrace"))
            (System/exit 1))
        (try
          (let [enriched (keep (fn [frame]
                                 (try
                                   (let [file (:file frame)
                                         line (:line frame)
                                         zloc (z/of-file file)
                                         form-match (z/find-depth-first zloc
                                                                       #(= (-> % z/node meta :row) line))]
                                     (if form-match
                                       (assoc frame :form (z/string form-match))
                                       frame))
                                   (catch Exception _ frame)))
                               frames)]
            (print-json (ok-response "trace"
                                    {:frames enriched})))
          (catch Exception e
            (do (print-json (error-response "enrichment-error" (.getMessage e)))
                (System/exit 1))))))
    (catch Exception e
      (do (print-json (error-response "parse-error" (.getMessage e)))
          (System/exit 1)))))

;; ============================================================================
;; CLI Dispatch
;; ============================================================================

(defn main [args]
  (cond
    (< (count args) 1)
    (do (print-json (error-response "invalid-args"
                                   "Usage: clj-lens.bb [--symbol|--find|--last-error|--trace] <args...>"))
        (System/exit 1))

    ;; Coordinate mode: clj-lens.bb <file> <line>
    (and (= (count args) 2)
         (not (str/starts-with? (first args) "--")))
    (coordinate-mode (first args) (second args))

    ;; Symbol mode: clj-lens.bb --symbol <ns/name>
    (and (>= (count args) 2)
         (= (first args) "--symbol"))
    (symbol-mode (second args))

    ;; Find mode: clj-lens.bb --find <pattern>
    (and (>= (count args) 2)
         (= (first args) "--find"))
    (find-mode (second args))

    ;; Last error mode: clj-lens.bb --last-error
    (= (first args) "--last-error")
    (last-error-mode)

    ;; Trace mode: clj-lens.bb --trace <stacktrace>
    (and (>= (count args) 2)
         (= (first args) "--trace"))
    (trace-mode (str/join " " (rest args)))

    ;; Invalid mode
    :else
    (do (print-json (error-response "invalid-mode"
                                   (str "Unknown mode: " (first args))))
        (System/exit 1))))

(main *command-line-args*)