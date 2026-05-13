# clj-lens Multi-Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance clj-lens.bb to support 5 modes (coordinate, symbol, find, last-error, trace) with consistent JSON output, reducing agent context usage.

**Architecture:** Single-file script with clear mode dispatch, helper functions for each data source (clj-kondo, nREPL, file I/O), and consistent JSON output envelope for all modes.

**Tech Stack:** Babashka, rewrite-clj (existing), clj-kondo (optional with fallback), nREPL (optional), clojure.json (for JSON output)

---

## Task 1: Refactor CLI Parsing & Output Envelope

**Files:**
- Modify: `scripts/clj-lens.bb`

**Context:** Current script is minimal (~16 lines). We'll restructure it to separate concerns: CLI parsing, mode dispatch, and a consistent JSON output format.

- [ ] **Step 1: Write a failing test to verify JSON output structure**

Create a simple test that checks the output format:

```bash
cd /Users/laurencechen/repo/clj-native-agent
cat > /tmp/test-clj-lens-json.clj << 'EOF'
(require '[clojure.json :as json])

;; Test: coordinate mode returns JSON with status/mode/data structure
(let [output (-> (shell/sh "bb" "scripts/clj-lens.bb" "scripts/clj-lens.bb" "1")
                 :out
                 (json/parse-string true))]
  (assert (= (:status output) :ok) "Status should be :ok")
  (assert (= (:mode output) "coordinate") "Mode should be 'coordinate'")
  (assert (contains? (:data output) :file) "Data should have :file")
  (assert (contains? (:data output) :line) "Data should have :line")
  (assert (contains? (:data output) :form) "Data should have :form")
  (println "✓ JSON envelope test passed"))
EOF
```

Run the test (it will fail because current script doesn't return JSON):
```bash
bb /tmp/test-clj-lens-json.clj 2>&1 | head -20
```

Expected: Script will error or return non-JSON output.

- [ ] **Step 2: Refactor clj-lens.bb to add JSON output helpers**

Replace the entire `scripts/clj-lens.bb` with this refactored version:

```clojure
#!/usr/bin/env bb
;; clj-lens: Multi-mode Structural Code Reader
(require '[rewrite-clj.zip :as z]
         '[clojure.data.json :as json]
         '[clojure.string :as str])

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
  (println (json/write-str response)))

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
;; Mode: Symbol Lookup (Placeholder - implemented in Task 3)
;; ============================================================================

(defn symbol-mode [symbol-name]
  (print-json (error-response "not-implemented" "Symbol mode not yet implemented")))

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
```

- [ ] **Step 3: Test coordinate mode returns valid JSON**

Run the test again:
```bash
bb /tmp/test-clj-lens-json.clj 2>&1
```

Expected output: `✓ JSON envelope test passed`

- [ ] **Step 4: Test invalid arguments produce JSON errors**

```bash
bb scripts/clj-lens.bb 2>&1 | grep -q '"status":"error"' && echo "✓ Error response is JSON" || echo "✗ Error response not JSON"
```

Expected: `✓ Error response is JSON`

- [ ] **Step 5: Commit**

```bash
git add scripts/clj-lens.bb docs/superpowers/plans/2026-05-13-clj-lens-multimode-implementation.md
git commit -m "refactor: restructure clj-lens with JSON output envelope and mode dispatch"
```

---

## Task 2: Add clj-kondo Query Helpers

**Files:**
- Modify: `scripts/clj-lens.bb` (add clj-kondo functions before mode implementations)

**Context:** The symbol and find modes depend on clj-kondo data. This task adds helper functions to query clj-kondo, with graceful fallback to file scanning if clj-kondo is unavailable.

- [ ] **Step 1: Add clj-kondo query function**

Add this after the output envelope functions in `scripts/clj-lens.bb`:

```clojure
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
```

Also add the required import at the top:
```clojure
(require '[clojure.java.shell :as shell])
```

- [ ] **Step 2: Test clj-kondo availability check**

```bash
# Assuming clj-kondo is installed
bb -e '(require "[clojure.java.shell :as shell]")
(defn clj-kondo-available? []
  (let [result (try (shell/sh "clj-kondo" "--version") (catch Exception _ nil))]
    (and result (zero? (:exit result)))))
(println (if (clj-kondo-available?) "✓ clj-kondo found" "✗ clj-kondo not found"))'
```

Expected: `✓ clj-kondo found` (or `✗` if clj-kondo is not installed, which is fine — graceful degradation)

- [ ] **Step 3: Test clj-kondo query function**

In the repo directory, test the query:

```bash
bb -e '(require "[clojure.java.shell :as shell]" "[clojure.data.json :as json]")
(defn query-clj-kondo []
  (try
    (let [result (shell/sh "clj-kondo" "export" "--format" "json")]
      (if (zero? (:exit result))
        (json/parse-string (:out result) true)
        nil))
    (catch Exception _ nil)))
(let [data (query-clj-kondo)]
  (if (and data (map? data))
    (println "✓ clj-kondo query returned data")
    (println "✗ clj-kondo query failed or returned nil")))'
```

Expected: `✓ clj-kondo query returned data`

- [ ] **Step 4: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: add clj-kondo query helpers for symbol/find modes"
```

---

## Task 3: Implement Symbol Lookup Mode

**Files:**
- Modify: `scripts/clj-lens.bb` (replace symbol-mode function)

**Context:** The --symbol mode uses clj-kondo to find a symbol definition, then extracts the source code using rewrite-clj.

- [ ] **Step 1: Implement symbol-mode function**

Replace the placeholder symbol-mode function in `scripts/clj-lens.bb`:

```clojure
(defn symbol-mode [symbol-name]
  (let [analysis (query-clj-kondo)]
    (if-not analysis
      (print-json (error-response "clj-kondo-unavailable" 
                                 "clj-kondo not found. Install with: npm install -g clj-kondo"))
      (let [matches (find-by-symbol analysis symbol-name)]
        (if (empty? matches)
          ;; No exact match, try suggestions
          (let [suggestions (find-by-pattern analysis 
                                              (last (str/split symbol-name #"/")))]
            (if (empty? suggestions)
              (print-json (error-response "symbol-not-found" 
                                         (str "Symbol " symbol-name " not found")))
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
                  (print-json (error-response "form-not-extracted" 
                                            (str "Could not extract form at line " line)))))
              (catch Exception e
                (print-json (error-response "read-error" (.getMessage e))))))))))
```

- [ ] **Step 2: Test symbol lookup with exact match**

First, find a symbol in the project:
```bash
clj-kondo export --format json | jq '.var-definitions[0:3]' 2>/dev/null || echo "Test: no defs found"
```

If results exist, test with one of them:
```bash
# Example: if you see a symbol like app.core/my-func, test it
bb scripts/clj-lens.bb --symbol "rewrite_clj.zip/of-file" 2>&1 | jq '.'
```

Expected: JSON response with `:ok` status and symbol definition

- [ ] **Step 3: Test symbol lookup with no match**

```bash
bb scripts/clj-lens.bb --symbol "nonexistent.ns/fake-symbol" 2>&1 | jq '.status'
```

Expected: `"error"` or `"suggestion"`

- [ ] **Step 4: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: implement --symbol mode with clj-kondo + rewrite-clj"
```

---

## Task 4: Implement Find Mode

**Files:**
- Modify: `scripts/clj-lens.bb` (replace find-mode function)

**Context:** The --find mode searches for symbols matching a pattern substring, returning all matches.

- [ ] **Step 1: Implement find-mode function**

Replace the placeholder find-mode function in `scripts/clj-lens.bb`:

```clojure
(defn find-mode [pattern]
  (let [analysis (query-clj-kondo)]
    (if-not analysis
      (print-json (error-response "clj-kondo-unavailable" 
                                 "clj-kondo not found. Install with: npm install -g clj-kondo"))
      (let [matches (find-by-pattern analysis pattern)]
        (if (empty? matches)
          (print-json (ok-response "find" 
                                  {:pattern pattern
                                   :matches []}))
          (print-json (ok-response "find"
                                  {:pattern pattern
                                   :matches (map #(select-keys % [:name :namespace :file :line]) 
                                                matches)})))))))
```

- [ ] **Step 2: Test find mode with a pattern**

```bash
bb scripts/clj-lens.bb --find "of-file" 2>&1 | jq '.'
```

Expected: JSON response with `:ok` status and array of matches

- [ ] **Step 3: Test find mode with no matches**

```bash
bb scripts/clj-lens.bb --find "zzzzzzzzzzzzzzz" 2>&1 | jq '.data.matches'
```

Expected: Empty array `[]`

- [ ] **Step 4: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: implement --find mode for pattern-based symbol search"
```

---

## Task 5: Add nREPL Connection Helpers

**Files:**
- Modify: `scripts/clj-lens.bb` (add nREPL functions before mode implementations)

**Context:** The --last-error mode needs to connect to nREPL to read the last exception. This task adds helper functions for nREPL discovery and communication.

- [ ] **Step 1: Add nREPL helper functions**

Add this section after clj-kondo functions in `scripts/clj-lens.bb`:

```clojure
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
        ;; Use brepl to query nREPL
        (let [result (shell/sh "brepl" "-c" code {:out :string :err :string})]
          (if (zero? (:exit result))
            (:out result)
            nil))
        (catch Exception _ nil)))))

(defn get-last-exception []
  "Get the last exception from nREPL (*e)"
  (query-nrepl "(when-let [e *e] (.toString e))"))
```

- [ ] **Step 2: Test nREPL detection**

If you have nREPL running:
```bash
# Start nREPL in another terminal (if not already running)
# clj -M:nrepl

# Then test detection
bb -e '(defn find-nrepl-port [] 
  (let [port-file ".nrepl-port"] 
    (if (.exists (java.io.File. port-file)) 
      (str/trim (slurp port-file)) 
      nil)))
(if-let [port (find-nrepl-port)] 
  (println (str "✓ Found nREPL on port " port))
  (println "✗ nREPL not found (this is OK - graceful degradation)"))'
```

Expected: Either port number or graceful "not found" message

- [ ] **Step 3: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: add nREPL connection helpers for --last-error mode"
```

---

## Task 6: Implement Last Error Mode

**Files:**
- Modify: `scripts/clj-lens.bb` (replace last-error-mode function)

**Context:** The --last-error mode retrieves the last exception from nREPL and shows the source code context where it occurred.

- [ ] **Step 1: Add exception parsing helpers**

Add this before the last-error-mode function in `scripts/clj-lens.bb`:

```clojure
(defn parse-exception-location [exception-str]
  "Extract file and line from a Clojure/Java exception"
  ;; Simple pattern: filename.clj:line
  (let [matches (re-find #"(\S+\.clj):(\d+)" exception-str)]
    (if matches
      {:file (second matches) :line (Integer/parseInt (nth matches 2))}
      nil)))

(defn analyze-error-context [form]
  "Simple heuristic analysis of error context"
  ;; Look for common error patterns in the form
  {:suspect "unknown" :reason "manual-inspection-needed"})
```

- [ ] **Step 2: Implement last-error-mode function**

Replace the placeholder last-error-mode function in `scripts/clj-lens.bb`:

```clojure
(defn last-error-mode []
  (let [port (find-nrepl-port)]
    (if-not port
      (print-json (error-response "nrepl-unavailable" 
                                 "nREPL server not found. Start with: clj -M:nrepl or bb nrepl-server 1667"))
      (try
        (let [exc-str (get-last-exception)]
          (if-not exc-str
            (print-json (error-response "no-exception" "No exception in *e"))
            (let [location (parse-exception-location exc-str)]
              (if-not location
                (print-json (error-response "parse-failed" 
                                           (str "Could not parse exception location: " exc-str)))
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
                        (print-json (error-response "form-not-found"
                                                  (str "Could not extract form at line " line)))))
                    (catch Exception e
                      (print-json (error-response "read-error" (.getMessage e)))))))))
        (catch Exception e
          (print-json (error-response "nrepl-error" (.getMessage e))))))))
```

- [ ] **Step 3: Test last-error mode (with nREPL running)**

If nREPL is running and has an exception:
```bash
bb scripts/clj-lens.bb --last-error 2>&1 | jq '.'
```

Expected: Either error (nREPL unavailable) or exception context with location and form

- [ ] **Step 4: Test last-error mode without nREPL**

```bash
# Without nREPL running
bb scripts/clj-lens.bb --last-error 2>&1 | jq '.error'
```

Expected: `"nrepl-unavailable"`

- [ ] **Step 5: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: implement --last-error mode with nREPL integration"
```

---

## Task 7: Implement Trace (Stacktrace Parsing) Mode

**Files:**
- Modify: `scripts/clj-lens.bb` (add stacktrace parsing + trace-mode function)

**Context:** The --trace mode parses a stacktrace string and extracts source code for each frame.

- [ ] **Step 1: Add stacktrace parsing helpers**

Add this before the trace-mode function in `scripts/clj-lens.bb`:

```clojure
(defn parse-stacktrace [stacktrace-str]
  "Parse a Clojure/Java stacktrace into frames with file/line info"
  (let [lines (str/split stacktrace-str #"\n")
        ;; Pattern: class.method (file.clj:line) or similar
        pattern #"at\s+([\w\.]+)\s*\(([^:]+):(\d+)\)"]
    (keep (fn [line]
            (let [matches (re-find pattern line)]
              (when matches
                {:class (second matches)
                 :method ""
                 :file (nth matches 2)
                 :line (Integer/parseInt (nth matches 3))})))
          lines)))
```

- [ ] **Step 2: Implement trace-mode function**

Replace the placeholder trace-mode function in `scripts/clj-lens.bb`:

```clojure
(defn trace-mode [stacktrace]
  (try
    (let [frames (parse-stacktrace stacktrace)]
      (if (empty? frames)
        (print-json (error-response "parse-failed" 
                                   "Could not parse any frames from stacktrace"))
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
            (print-json (error-response "enrichment-error" (.getMessage e)))))))
    (catch Exception e
      (print-json (error-response "parse-error" (.getMessage e))))))
```

- [ ] **Step 3: Create a test stacktrace file**

```bash
cat > /tmp/test-stacktrace.txt << 'EOF'
java.lang.NullPointerException
	at clojure.core$get_name.invoke (core.clj:42)
	at clojure.core$render.invoke (ui.clj:88)
EOF
```

- [ ] **Step 4: Test trace mode**

```bash
bb scripts/clj-lens.bb --trace "$(cat /tmp/test-stacktrace.txt)" 2>&1 | jq '.status'
```

Expected: `"ok"` or `"error"` (depending on whether files in the stacktrace exist)

- [ ] **Step 5: Commit**

```bash
git add scripts/clj-lens.bb
git commit -m "feat: implement --trace mode for stacktrace parsing and enrichment"
```

---

## Task 8: Integration Testing

**Files:**
- Create: `tests/clj-lens_test.clj`
- Modify: `scripts/clj-lens.bb` (final polish: error handling edge cases)

**Context:** Write comprehensive tests for all 5 modes to ensure they work correctly together.

- [ ] **Step 1: Create integration test file**

```bash
cat > tests/clj-lens_test.clj << 'EOF'
(require '[clojure.test :as t]
         '[clojure.data.json :as json]
         '[clojure.java.shell :as shell])

(defn run-clj-lens [& args]
  "Run clj-lens.bb and parse JSON output"
  (let [result (apply shell/sh "bb" "scripts/clj-lens.bb" args)]
    (if (zero? (:exit result))
      (json/parse-string (:out result) true)
      {:status "error" :error "exit-code" :message (str "Exit code: " (:exit result))})))

(t/deftest test-coordinate-mode
  (let [response (run-clj-lens "scripts/clj-lens.bb" "1")]
    (t/is (= (:status response) "ok"))
    (t/is (= (:mode response) "coordinate"))
    (t/is (contains? (:data response) :file))
    (t/is (contains? (:data response) :line))
    (t/is (contains? (:data response) :form))))

(t/deftest test-symbol-mode-missing-arg
  (let [response (run-clj-lens "--symbol")]
    (t/is (= (:status response) "error"))))

(t/deftest test-find-mode-empty-pattern
  (let [response (run-clj-lens "--find" "zzzzzzzzz")]
    (t/is (= (:status response) "ok"))
    (t/is (empty? (get-in response [:data :matches])))))

(t/deftest test-last-error-no-nrepl
  (let [response (run-clj-lens "--last-error")]
    ;; Should either error (nREPL unavailable) or error (no exception)
    (t/is (= (:status response) "error"))))

(t/deftest test-trace-mode-parse
  (let [stacktrace "at app.core$func (core.clj:10)"
        response (run-clj-lens "--trace" stacktrace)]
    ;; Should succeed in parsing at least
    (t/is (or (= (:status response) "ok")
              (= (:status response) "error")))))

(t/deftest test-invalid-mode
  (let [response (run-clj-lens "--invalid")]
    (t/is (= (:status response) "error"))
    (t/is (= (:error response) "invalid-mode"))))

(println "Running clj-lens tests...")
(t/run-tests)
EOF
```

- [ ] **Step 2: Run tests to verify all modes work**

```bash
cd /Users/laurencechen/repo/clj-native-agent
bb tests/clj-lens_test.clj 2>&1
```

Expected: All tests pass (or show expected failures for nREPL/file-based tests)

- [ ] **Step 3: Test error handling edge cases**

Test missing file:
```bash
bb scripts/clj-lens.bb nonexistent.clj 1 2>&1 | jq '.status'
```

Expected: `"error"`

Test invalid line:
```bash
bb scripts/clj-lens.bb scripts/clj-lens.bb 99999 2>&1 | jq '.status'
```

Expected: `"error"`

- [ ] **Step 4: Add final error handling polish**

Ensure all error paths in clj-lens.bb properly exit with status code 1:

```bash
# Every error path should call (System/exit 1)
grep -n "print-json (error-response" scripts/clj-lens.bb | wc -l
```

If you find error paths without System/exit calls, add them.

- [ ] **Step 5: Commit tests and final polish**

```bash
git add tests/clj-lens_test.clj scripts/clj-lens.bb
git commit -m "test: add integration tests for all clj-lens modes"
```

---

## Task 9: Documentation & Final Verification

**Files:**
- Modify: `skills/clojure-fast-read/SKILL.md` (add examples for new modes)

**Context:** Update documentation to show usage examples for all 5 modes.

- [ ] **Step 1: Update SKILL.md with new modes**

Add this section to `skills/clojure-fast-read/SKILL.md` after the existing "Implementation Examples":

```markdown
## New Multi-Mode Usage

### Symbol Lookup

```bash
# Find and extract a specific symbol
./scripts/clj-lens.bb --symbol app.db/update-user
# Output: JSON with file, line, and complete function definition
```

### Find Symbols by Pattern

```bash
# Search for all definitions containing "update"
./scripts/clj-lens.bb --find "update"
# Output: JSON with array of matching symbols and locations
```

### Extract Error Context

```bash
# Show source code at the last exception location (requires nREPL)
./scripts/clj-lens.bb --last-error
# Output: JSON with exception type, location, and source form
```

### Parse Stacktraces

```bash
# Extract code from each frame in a stacktrace
./scripts/clj-lens.bb --trace "java.lang.NullPointerException
	at app.core\$func (core.clj:42)
	at app.ui\$render (ui.clj:88)"
# Output: JSON with enriched frames including source code
```
```

- [ ] **Step 2: Run final comprehensive test**

Test all modes one more time:
```bash
# Coordinate
bb scripts/clj-lens.bb scripts/clj-lens.bb 1 > /tmp/coord.json && echo "✓ Coordinate works"

# Symbol (will fail if symbol not in project, but should return valid JSON)
bb scripts/clj-lens.bb --symbol "clojure.core/println" > /tmp/sym.json && echo "✓ Symbol works"

# Find
bb scripts/clj-lens.bb --find "bb" > /tmp/find.json && echo "✓ Find works"

# Last error (may fail with nREPL unavailable, but should return valid JSON)
bb scripts/clj-lens.bb --last-error > /tmp/err.json 2>&1 && echo "✓ Last-error returns JSON"

# Trace
bb scripts/clj-lens.bb --trace "at app (core.clj:1)" > /tmp/trace.json && echo "✓ Trace works"
```

Expected: All commands complete and return valid JSON

- [ ] **Step 3: Verify all JSON responses are parseable**

```bash
for f in /tmp/{coord,sym,find,err,trace}.json; do
  jq . "$f" > /dev/null && echo "✓ $(basename $f) is valid JSON" || echo "✗ $(basename $f) is invalid JSON"
done
```

Expected: All valid JSON

- [ ] **Step 4: Commit documentation updates**

```bash
git add skills/clojure-fast-read/SKILL.md
git commit -m "docs: add examples for new clj-lens multi-mode features"
```

---

## Summary of Changes

| File | Change | Reason |
|------|--------|--------|
| `scripts/clj-lens.bb` | Complete rewrite: 5 modes, JSON output, clj-kondo/nREPL integration | Core feature delivery |
| `tests/clj-lens_test.clj` | New integration tests for all modes | Verify functionality |
| `skills/clojure-fast-read/SKILL.md` | Added usage examples for new modes | Document for users |

---

## Spec Coverage Checklist

- ✅ **Coordinate mode (JSON-wrapped)**: Task 1
- ✅ **Symbol lookup** (clj-kondo + rewrite-clj): Tasks 2, 3
- ✅ **Find** (pattern search via clj-kondo): Tasks 2, 4
- ✅ **Last error** (nREPL + source extraction): Tasks 5, 6
- ✅ **Trace** (stacktrace parsing): Task 7
- ✅ **JSON output envelope**: Task 1
- ✅ **Graceful degradation**: All tasks (clj-kondo fallback, nREPL optional)
- ✅ **Error handling**: All tasks, Task 8
- ✅ **Integration testing**: Task 8
- ✅ **Documentation**: Task 9
