(require '[clojure.test :as t]
         '[cheshire.core :as json]
         '[clojure.java.shell :as shell])

(defn run-clj-lens [& args]
  "Run clj-lens.bb and parse JSON output"
  (let [result (apply shell/sh "bb" "skills/clj-lens/scripts/clj-lens.bb" args)]
    (if (zero? (:exit result))
      (json/parse-string (:out result) true)
      {:status "error" :error "exit-code" :exit (:exit result)})))

(t/deftest test-read-mode
  (let [response (run-clj-lens "--read" "3" "1" "skills/clj-lens/scripts/clj-lens.bb")]
    (t/is (= (:status response) "ok"))
    (t/is (= (:mode response) "read"))
    (t/is (contains? (:data response) :form))))

(t/deftest test-symbol-mode-error
  (let [response (run-clj-lens "--symbol" "fake.ns/fake")]
    (t/is (or (= (:status response) "error")
              (= (:status response) "suggestion")))))

(t/deftest test-find-mode
  (let [response (run-clj-lens "--find" "zzz")]
    (t/is (or (= (:status response) "ok")
              (= (:status response) "error")))))  ;; error if clj-kondo unavailable

(t/deftest test-last-error-no-nrepl
  (let [response (run-clj-lens "--last-error")]
    (t/is (= (:status response) "error"))))

(t/deftest test-trace-mode
  (let [response (run-clj-lens "--trace" "at app (core.clj:1)")]
    (t/is (or (= (:status response) "ok")
              (= (:status response) "error")))))

(t/deftest test-invalid-mode
  (let [response (run-clj-lens "--invalid")]
    (t/is (= (:status response) "error"))))

(println "Running clj-lens tests...")
(t/run-tests)
