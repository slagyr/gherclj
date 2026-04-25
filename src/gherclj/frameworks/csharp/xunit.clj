(ns gherclj.frameworks.csharp.xunit
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.framework :as fw]))

(defonce scenario-setup-registry (atom {}))
(defonce project-reference-registry (atom {}))

(defmacro scenario-setup! [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defmacro project-reference! [path]
  `(swap! project-reference-registry update '~(ns-name *ns*) (fnil conj []) ~path))

(defn csharp-string [s]
  (str "\""
       (-> (str s)
           (str/replace #"\\" "\\\\")
           (str/replace #"\"" "\\\"")
           (str/replace #"\n" "\\n")
           (str/replace #"\t" "\\t"))
       "\""))

(defn csharp-literal [value]
  (cond
    (string? value) (csharp-string value)
    (keyword? value) (csharp-string (name value))
    (symbol? value) (csharp-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "null"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "new[] { " (str/join ", " (map csharp-literal value)) " }")
    :else (csharp-string value)))

(defn- pascal [s]
  (->> (str/split s #"-")
       (remove str/blank?)
       (map str/capitalize)
       (apply str)))

(defn- upper-first [s]
  (if (seq s)
    (str (str/upper-case (subs s 0 1)) (subs s 1))
    s))

(defn csharp-method [name]
  (let [segments (str/split name #"\.")]
    (if (= 1 (count segments))
      (if (str/includes? (first segments) "-")
        (pascal (first segments))
        (upper-first (first segments)))
      (str/join "." (cons (first segments)
                           (map #(if (str/includes? % "-")
                                   (pascal %)
                                   (upper-first %))
                                (rest segments)))))))

(defn generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map csharp-literal all-args))]
    (str (csharp-method name) "(" args-str ");")))

(defmethod fw/render-step :csharp/xunit [_config step]
  (generate-step-call step))

(defn- using-line [import-val]
  (let [s (str/trim (str import-val))]
    (cond
      (str/starts-with? s "using ") (if (str/ends-with? s ";") s (str s ";"))
      :else (str "using " s ";"))))

(defn- normalize-path [path]
  (str/replace path "\\" "/"))

(defn- absolute-file [path]
  (let [f (io/file path)]
    (if (.isAbsolute f)
      f
      (io/file (System/getProperty "user.dir") path))))

(defn- relative-path [from-dir target]
  (normalize-path (str (.relativize (.toPath (absolute-file from-dir)) (.toPath (absolute-file target))))))

(defn- class-name [feature-name]
  (str (-> feature-name
           (str/replace #"[^A-Za-z0-9]+" "-")
           pascal)
       "Tests"))

(defn- method-name [scenario-name]
  (-> scenario-name
      (str/replace #"[^A-Za-z0-9]+" "-")
      pascal))

(defmethod fw/generate-preamble :csharp/xunit
  [_config source used-nses]
  (let [helper-imports (->> used-nses
                            (mapcat core/helper-imports-in-ns)
                            distinct
                            (map using-line)
                            sort)]
    (str (str/join "\n" (concat [(str "// generated from " source)]
                                   helper-imports
                                   ["using Xunit;"]))
         "\n\n"
         "namespace Generated\n"
         "{")))

(defmethod fw/wrap-feature :csharp/xunit
  [_config feature-name scenario-blocks]
  (str "    public class " (class-name feature-name) "\n"
       "    {\n"
       scenario-blocks "\n"
       "    }\n"
       "}\n"))

(defn- collect-scenario-setup [used-nses]
  (->> used-nses
       (mapcat #(get @scenario-setup-registry % []))
       distinct))

(defmethod fw/wrap-scenario :csharp/xunit
  [config scenario background]
  (let [used-nses (or (:_used-nses config) #{})
        setup-lines (collect-scenario-setup used-nses)
        bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat setup-lines bg-calls step-calls)]
    (str "        [Fact]\n"
         "        public void " (method-name (:scenario scenario)) "()\n"
         "        {\n"
         (str/join "\n" (map #(str "            " %) body-lines)) "\n"
         "        }")))

(defmethod fw/wrap-pending :csharp/xunit
  [_config scenario _background]
  (str "        [Fact(Skip = \"not yet implemented\")]\n"
       "        public void " (method-name (:scenario scenario)) "()\n"
       "        {\n"
       "        }"))

(defn- project-references [used-nses]
  (->> used-nses
       (mapcat #(get @project-reference-registry % []))
       distinct))

(defn- generate-csproj [output-dir used-nses]
  (let [project-refs (project-references used-nses)
        reference-lines (->> project-refs
                             (map #(str "    <ProjectReference Include=\"" (relative-path output-dir %) "\" />"))
                             (str/join "\n"))]
    (str "<Project Sdk=\"Microsoft.NET.Sdk\">\n"
         "  <PropertyGroup>\n"
         "    <TargetFramework>net5.0</TargetFramework>\n"
         "    <IsPackable>false</IsPackable>\n"
         "  </PropertyGroup>\n"
         "  <ItemGroup>\n"
         "    <PackageReference Include=\"Microsoft.NET.Test.Sdk\" Version=\"16.9.4\" />\n"
         "    <PackageReference Include=\"xunit\" Version=\"2.4.2\" />\n"
         "    <PackageReference Include=\"xunit.runner.visualstudio\" Version=\"2.4.5\">\n"
         "      <PrivateAssets>all</PrivateAssets>\n"
         "      <IncludeAssets>runtime; build; native; contentfiles; analyzers; buildtransitive</IncludeAssets>\n"
         "    </PackageReference>\n"
         "  </ItemGroup>\n"
         (when (seq project-refs)
           (str "  <ItemGroup>\n" reference-lines "\n  </ItemGroup>\n"))
         "</Project>\n")))

(defmethod fw/run-specs :csharp/xunit
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        used-nses (or (:_used-nses config) (set (:step-namespaces config)) #{})
        project-file (io/file output-dir "gherclj.generated.csproj")
        opts (or (:framework-opts config) [])]
    (io/make-parents project-file)
    (spit project-file (generate-csproj output-dir used-nses))
    (let [cmd (concat ["dotnet" "test" (str project-file) "--nologo"] opts)
          {:keys [exit out err]} (apply shell/sh cmd)]
      (when (seq out)
        (print out))
      (when (seq err)
        (binding [*out* *err*]
          (print err)))
      (when-not (zero? exit)
        (throw (ex-info "dotnet test failed" {:exit exit :stderr err})))
      0)))
