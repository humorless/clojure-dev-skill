# Clojure Idioms and Patterns

Reference for idiomatic Clojure code.

## Threading Macros

### -> (thread-first)

For object/map transformations:

```clojure
(-> user
    (assoc :last-login (Instant/now))
    (update :login-count inc)
    (dissoc :temporary-token))
```

### ->> (thread-last)

For sequence operations:

```clojure
(->> users
     (filter active?)
     (map :email)
     (remove nil?)
     (str/join ", "))
```

### some->

Short-circuit on nil:

```clojure
(some-> user :address :postal-code (subs 0 5))
```

### cond->

Conditional transformations:

```clojure
(cond-> request
  authenticated? (assoc :user current-user)
  admin?         (assoc :permissions :all))
```

Keep pipelines to 3–7 steps. Break up longer chains.

## Control Flow

### when

Single-branch with side effects:

```clojure
(when (valid-input? data)
  (log-event "Processing")
  (process data))
```

### cond

Multiple conditions:

```clojure
(cond
  (< n 0) :negative
  (= n 0) :zero
  :else   :positive)
```

### case

Constant dispatch:

```clojure
(case operation
  :add      (+ a b)
  :subtract (- a b)
  (throw (ex-info "Unknown op" {:op operation})))
```

## Data Structures

Prefer plain maps with keyword keys:

```clojure
{:id 123 :email "user@example.com" :roles #{:admin}}
```

Use destructuring:

```clojure
(defn format-user [{:keys [first-name last-name email]}]
  (str last-name ", " first-name " <" email ">"))

(defn connect [{:keys [host port] :or {port 8080}}]
  (create-connection host port))
```

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Functions/vars | kebab-case | `calculate-total`, `max-retries` |
| Predicates | suffix `?` | `valid?`, `active?` |
| Conversions | source->target | `map->vector`, `string->int` |
| Side-effectful functions | suffix `!` | `save-user!`, `delete-records!` |
| Dynamic vars | earmuffs | `*connection*` |
| Private helpers | prefix `-` | `-parse-date` |
| Unused bindings | underscore | `_request` |

## Code Layout

- Line length: under 80 characters
- Indentation: 2 spaces, never tabs
- Closing parens: gather on single line

```clojure
;; Good
(when something
  (something-else))

;; Bad
(when something
  (something-else)
)
```

## Namespace Structure

```clojure
(ns project.module
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [project.db :as db])
  (:import
   (java.time LocalDate)))

(set! *warn-on-reflection* true)
```

Standard aliases:
- `str` for `clojure.string`
- `set` for `clojure.set`
- `io` for `clojure.java.io`

## Docstrings

Required for all public functions:

```clojure
(defn calculate-total
  "Calculate total price including tax.

   Args:
     price - base price as BigDecimal
     rate  - tax rate as decimal (0.08 = 8%)

   Returns:
     BigDecimal total price

   Example:
     (calculate-total 100.00M 0.08) => 108.00M"
  [price rate]
  ...)
```

## Error Handling

```clojure
(try
  (slurp "file.txt")
  (catch java.io.FileNotFoundException e
    (log/error "File not found" {:path "file.txt"})
    nil)
  (catch Exception e
    (log/error "Unexpected error" {:error e})
    (throw e)))
```

Use `ex-info` with structured data:

```clojure
(throw (ex-info "Validation failed"
                {:field :email
                 :value input
                 :reason "Invalid format"}))
```

## Testing

```clojure
(deftest function-name-test
  (testing "happy path"
    (is (= expected (function input))))
  (testing "nil input"
    (is (nil? (function nil))))
  (testing "empty collection"
    (is (= [] (function [])))))
```

Coverage: happy path, nil, empty, boundary values, invalid types.

## Common Patterns

### Function Composition

```clojure
(def process-data
  (comp transform validate parse))
```

### Higher-Order Functions

```clojure
(->> items
     (filter valid?)
     (map transform)
     (reduce combine))
```

### Memoization

```clojure
(def expensive-fn
  (memoize (fn [x] ...)))
```

### Multi-Arity Functions

```clojure
(defn greet
  ([name] (greet name "Hello"))
  ([name greeting] (str greeting ", " name)))
```

## Anti-Patterns to Avoid

```clojure
;; Bad: mutable atom for accumulation
(defn bad-sum [nums]
  (let [acc (atom 0)]
    (doseq [n nums] (swap! acc + n))
    @acc))

;; Good: use reduce
(defn good-sum [nums]
  (reduce + nums))

;; Bad: nested nil checks
(if user
  (if (:address user)
    (if (:zip (:address user))
      (:zip (:address user)))))

;; Good: use some->
(some-> user :address :zip)
```
