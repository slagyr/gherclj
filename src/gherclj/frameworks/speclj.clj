(ns gherclj.frameworks.speclj
  (:require [clojure.string :as str]
            [gherclj.core :as g]
            [gherclj.framework :as fw]
            [gherclj.generator :as gen]
            [gherclj.lifecycle :as lifecycle]
            [speclj.cli :as speclj]
            [speclj.core :as sc]))

(defmethod fw/generate-preamble :speclj
  [_config source used-nses]
  (let [ns-name (str (gen/source->ns-name source "-spec"))
        helper-imports (->> used-nses
                            (mapcat g/helper-imports-in-ns)
                            distinct)
        helper-reqs (->> helper-imports
                         sort
                         (map #(str "            [" % " :as " (gen/ns->alias %) "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [speclj.core :refer :all]\n"
         "            [gherclj.core :as g]\n"
         "            [gherclj.lifecycle :as lifecycle]"
         (when (seq helper-reqs)
           (str "\n" (str/join "\n" helper-reqs)))
         "))")))

(defmethod fw/wrap-feature :speclj
  [_config feature-name scenario-blocks]
  (str "(describe \"" feature-name "\"\n\n"
       "  (before-all (lifecycle/run-before-feature-hooks!))\n"
       "  (before (g/reset!) (lifecycle/run-before-scenario-hooks!))\n"
       "  (after (lifecycle/run-after-scenario-hooks!))\n"
       "  (after-all (lifecycle/run-after-feature-hooks!))\n\n"
       scenario-blocks ")\n"))

(defmethod fw/wrap-scenario :speclj
  [_config scenario background]
  (let [title      (:scenario scenario)
        bg-calls   (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body       (->> (concat bg-calls step-calls)
                        (map #(str "    " %))
                        (str/join "\n"))]
    (str "  (it \"" title "\"\n"
         body ")")))

(defmethod fw/wrap-pending :speclj
  [_config scenario background]
  (let [title (:scenario scenario)
        step-comments (->> (concat (when background
                                     (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                          (:steps background)))
                                   (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                        (:steps scenario)))
                           (map #(str "    " %))
                           (str/join "\n"))]
    (str "  (it \"" title "\"\n"
         step-comments "\n"
         "    (pending \"not yet implemented\"))")))

(defn run-args
  [{:keys [output-dir framework-opts]
    :or {output-dir "target/gherclj/generated"}}]
  (into ["-c" output-dir "-s" "src"] (or framework-opts [])))

(defmethod fw/run-specs :speclj
  [config]
  (g/set-framework! :speclj)
  (let [args (run-args config)]
    (lifecycle/run-before-all-hooks!)
    (try
      (apply speclj/run args)
      (finally
        (lifecycle/run-after-all-hooks!)))))

(defmethod g/should= :speclj [expected actual] (sc/should= expected actual))
(defmethod g/should :speclj [value] (sc/should value))
(defmethod g/should-not :speclj [value] (sc/should-not value))
(defmethod g/should-be-nil :speclj [value] (sc/should-be-nil value))
(defmethod g/should-not-be-nil :speclj [value] (sc/should-not-be-nil value))
