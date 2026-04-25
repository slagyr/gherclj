(ns gherclj.features.steps.code-generation
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.generator :as gen]
            [gherclj.sample.app-steps]
            [clojure.string :as str]))

(helper! gherclj.features.steps.code-generation)

(def ^:private pipeline-base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defn setup-feature! [name source]
  (g/assoc! :feature-ir {:feature name :source source :scenarios []}))

(defn add-scenario! [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/update-in! [:feature-ir :scenarios] conj
                  {:scenario title :steps steps})))

(defn add-background! [table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (g/assoc-in! [:feature-ir :background] {:steps steps})))

(defn add-wip-scenario! [title table]
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

(defn generate-spec! [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (ensure-framework-loaded! fw)
    (let [config {:step-namespaces ['gherclj.sample.app-steps]
                  :extra-steps (g/get :steps)
                  :framework fw}]
      (g/assoc! :generated-output (gen/generate-spec config (g/get :feature-ir))))))

(defn output-should-contain [expected]
  (let [expected (str/replace expected #"^\"|\"$" "")
        raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (str/replace raw-output (str pipeline-base-dir "/") "")]
    (g/should (str/includes? output expected))))

(defn output-should-not-contain [text]
  (let [raw-output (or (g/get :cli-output) (g/get :generated-output) (g/get :pipeline-output) "")
        output (str/replace raw-output (str pipeline-base-dir "/") "")]
    (g/should-not (str/includes? output text))))

(defn generated-code-should-be [doc-string]
  (g/should= (str/trim doc-string) (str/trim (g/get :generated-output))))

(defgiven "a feature named {name:string} from source {source:string}" code-generation/setup-feature!
  "Initializes :feature-ir with an empty scenarios list. source is the .feature file path embedded in generated code.")

(defgiven "a scenario {title:string} with steps:" code-generation/add-scenario!)

(defgiven "a background with steps:" code-generation/add-background!)

(defgiven "a wip scenario {title:string} with steps:" code-generation/add-wip-scenario!
  "Adds scenario with [\"wip\"] tag hardcoded — simulates a scenario tagged @wip.")

(defwhen "generating the spec with framework {framework}" code-generation/generate-spec!
  "Uses gherclj.sample.app-steps as hardcoded step namespace, merged with any :extra-steps in state.")

(defthen #"^the output should contain (?!lines:$)(.+)$" code-generation/output-should-contain)

(defthen "the output should not contain {text:string}" code-generation/output-should-not-contain
  "Checks :cli-output → :generated-output → :pipeline-output in fallback order. Strips pipeline temp dir prefix from paths.")

(defthen "the generated code should be:" code-generation/generated-code-should-be)
