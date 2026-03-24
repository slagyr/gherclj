(ns gherclj.frameworks.speclj
  (:require [clojure.string :as str]
            [gherclj.generator :as gen]))

(defmethod gen/generate-ns-form :speclj
  [config source step-ns-syms harness-ns]
  (let [ns-name (str (#'gen/source->ns-name source "-spec"))
        harness-req (str "            [" harness-ns " :as h]")
        step-reqs (->> step-ns-syms
                       sort
                       (map #(str "            [" % " :as " (gen/ns->alias %) "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [speclj.core :refer :all]\n"
         harness-req
         (when (seq step-reqs)
           (str "\n" (str/join "\n" step-reqs)))
         "))")))

(defmethod gen/wrap-feature :speclj
  [_config feature-name scenario-blocks]
  (str "(describe \"" feature-name "\"\n\n"
       scenario-blocks ")\n"))

(defmethod gen/wrap-scenario :speclj
  [_config scenario background]
  (let [title (:scenario scenario)
        bg-calls (when background
                   (->> (:steps background)
                        (filter :classified?)
                        (map #'gen/generate-step-call-with-extras)))
        step-calls (->> (:steps scenario)
                        (map #'gen/generate-step-call-with-extras))
        all-calls (concat ["(h/reset!)"] bg-calls step-calls)
        body (->> all-calls
                  (map #(str "      " %))
                  (str/join "\n"))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         body "))")))

(defmethod gen/wrap-pending :speclj
  [_config scenario background]
  (let [title (:scenario scenario)
        step-comments (->> (concat (when background
                                     (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                          (:steps background)))
                                   (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                        (:steps scenario)))
                           (map #(str "      " %))
                           (str/join "\n"))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         step-comments "\n"
         "      (pending \"not yet implemented\")))")))
