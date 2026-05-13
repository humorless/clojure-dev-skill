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
;; Mode: Find (Placeholder - implemented in Task 4)
;; ============================================================================

(defn find-mode [pattern]
  (print-json (error-response "not-implemented" "Find mode not yet implemented")))

;; ============================================================================
;; Mode: Last Error (Placeholder - implemented in Task 5)
;; ============================================================================

(defn last-error-mode []
  (print-json (error-response "not-implemented" "Last-error mode not yet implemented")))

;; ============================================================================
;; Mode: Trace (Placeholder - implemented in Task 6)
;; ============================================================================

(defn trace-mode [stacktrace]
  (print-json (error-response "not-implemented" "Trace mode not yet implemented")))

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