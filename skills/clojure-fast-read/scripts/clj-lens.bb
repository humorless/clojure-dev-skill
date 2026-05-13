#!/usr/bin/env bb
;; Usage: clj-lens src/core.clj 49
(require '[rewrite-clj.zip :as z])

(try
  (let [[path line] *command-line-args*
        zloc (z/of-file path)
        row (Integer/parseInt line)
        ;; search until the line number match
        match (z/find-depth-first zloc #(= (-> % z/node meta :row) row))]
    (if match
      (println (z/string match))
      (System/exit 1)))
  (catch Exception e
    (binding [*out* *err*] (println (.getMessage e)))
    (System/exit 1)))
