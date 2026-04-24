(ns gherclj.features.steps.code-generation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.generator :as gen]
            [gherclj.sample.app-steps]
            [clojure.string :as str]))

(def ^:private pipeline-base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defgiven setup-feature "a feature named {name:string} from source {source:string}"
  [name source]
  (g/assoc! :feature-ir {:feature name :source source :scenarios []}))

(defgiven add-scenario "a scenario {title:string} with steps:"
  [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/update-in! [:feature-ir :scenarios] conj
                  {:scenario title :steps steps})))

(defgiven add-background "a background with steps:"
  [table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/assoc-in! [:feature-ir :background] {:steps steps})))

(defgiven add-wip-scenario "a wip scenario {title:string} with steps:"
  [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/update-in! [:feature-ir :scenarios] conj
                  {:scenario title :steps steps :tags ["wip"]})))

(defn- ensure-framework-loaded! [fw]
  (let [fw-ns (case fw
                :speclj 'gherclj.frameworks.speclj
                :clojure.test 'gherclj.frameworks.clojure-test
                (symbol (str "gherclj.frameworks." (name fw))))]
    (require fw-ns)))

(defwhen generate-spec "generating the spec with framework {framework}"
  [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (ensure-framework-loaded! fw)
    (let [config {:step-namespaces ['gherclj.sample.app-steps]
                  :extra-steps (g/get :steps)
                  :test-framework fw}]
      (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir))))))

(defthen output-should-contain #"^the output should contain (?!lines:$)(.+)$"
  [expected]
  (let [expected (str/replace expected #"^\"|\"$" "")
        raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (str/replace raw-output (str pipeline-base-dir "/") "")]
    (g/should (str/includes? output expected))))

(defthen output-should-not-contain "the output should not contain {text:string}"
  [text]
  (let [raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (str/replace raw-output (str pipeline-base-dir "/") "")]
    (g/should-not (str/includes? output text))))

(defthen generated-code-should-be "the generated code should be:"
  [doc-string]
  (g/should= (str/trim doc-string) (str/trim (g/get :generated-output))))
