(ns gherclj.lifecycle)

(def ^:private stages
  [:before-all :before-feature :before-scenario :after-scenario :after-feature :after-all])

(defonce ^:private hooks
  (atom (zipmap stages (repeat []))))

(defn register! [stage f]
  (swap! hooks update stage conj f)
  nil)

(defn clear!
  "Clear all registered lifecycle hooks. Intended for tests."
  []
  (reset! hooks (zipmap stages (repeat []))))

(defn run! [stage]
  (doseq [f (get @hooks stage)]
    (f)))

(defn run-before-all-hooks! [] (run! :before-all))
(defn run-before-feature-hooks! [] (run! :before-feature))
(defn run-before-scenario-hooks! [] (run! :before-scenario))
(defn run-after-scenario-hooks! [] (run! :after-scenario))
(defn run-after-feature-hooks! [] (run! :after-feature))
(defn run-after-all-hooks! [] (run! :after-all))
