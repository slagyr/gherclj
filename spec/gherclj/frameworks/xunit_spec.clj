(ns gherclj.frameworks.csharp.xunit-spec
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.sample.csharp-app-steps]
            [gherclj.framework :as fw]
            [gherclj.frameworks.csharp.xunit :as xunit]
            [speclj.core :refer :all]))

(xunit/project-reference! "lib/SampleApp.csproj")
(xunit/scenario-setup! "var subject = new SampleAppSteps();")

(describe "xUnit framework"

  (context "generate-preamble"

    (it "generates C# preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :csharp/xunit}
                                         "features/auth.feature"
                                         ['gherclj.sample.csharp-app-steps])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "using SampleApp;"))
        (should (str/includes? result "using Xunit;"))
        (should (str/includes? result "namespace Generated"))))

    (it "omits custom using lines when step namespace has none"
      (let [result (fw/generate-preamble {:framework :csharp/xunit}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "using Xunit;"))
        (should-not (str/includes? result "using SampleApp;")))))

  (context "render-step"

    (it "renders a camelCase helper ref as a PascalCase method call"
      (let [step {:name "subject.createAdventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "subject.CreateAdventurer(\"alice\");" (fw/render-step {:framework :csharp/xunit} step))))

    (it "renders a no-arg step with parentheses"
      (let [step {:name "subject.enterTheRealm" :args [] :ns 'myapp.steps}]
        (should= "subject.EnterTheRealm();" (fw/render-step {:framework :csharp/xunit} step))))

    (it "renders integer args as C# literals"
      (let [step {:name "subject.verifyOutcome" :args [200] :ns 'myapp.steps}]
        (should= "subject.VerifyOutcome(200);" (fw/render-step {:framework :csharp/xunit} step)))))

  (context "wrap-scenario"

    (it "wraps pre-rendered lines in a Fact method"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["SeedUser(\"alice\");"
                                       "LogIn();"]}
            result (fw/wrap-scenario {:framework :csharp/xunit} scenario nil)]
        (should (str/includes? result "[Fact]"))
        (should (str/includes? result "public void UserCanLogIn()"))
        (should (str/includes? result "SeedUser(\"alice\");"))
        (should (str/includes? result "LogIn();"))))

    (it "includes scenario setup and background steps before scenario steps"
      (let [background {:rendered-steps ["CleanDb();"]}
            scenario {:scenario "User can log in"
                      :rendered-steps ["LogIn();"]}
            result (fw/wrap-scenario {:framework :csharp/xunit
                                      :_used-nses ['gherclj.frameworks.csharp.xunit-spec]}
                                     scenario background)]
        (should (str/includes? result "var subject = new SampleAppSteps();"))
        (should (str/includes? result "CleanDb();"))
        (should (str/includes? result "LogIn();")))))

  (context "run-specs"

    (it "creates a generated xUnit project and runs dotnet test against it"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-csharp-framework-spec")
            output-dir (io/file tmp-dir "generated")
            test-file (io/file output-dir "a_test.cs")
            captured (atom nil)]
        (io/make-parents test-file)
        (spit test-file "public class A {}")
        (try
          (with-redefs [clojure.java.shell/sh (fn [& args]
                                                (reset! captured args)
                                                {:exit 0 :out "" :err ""})]
            (fw/run-specs {:framework :csharp/xunit
                           :output-dir (str output-dir)
                           :_used-nses ['gherclj.frameworks.csharp.xunit-spec]}))
          (should= ["dotnet" "test" (str (io/file output-dir "gherclj.generated.csproj")) "--nologo"] @captured)
          (should (.exists (io/file output-dir "gherclj.generated.csproj")))
          (finally
            (.delete test-file)
            (.delete (io/file output-dir "gherclj.generated.csproj"))
            (.delete output-dir)
            (.delete (io/file tmp-dir))))))

    (it "prints dotnet stdout and stderr"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-csharp-framework-stdout")
            output-dir (io/file tmp-dir "generated")
            test-file (io/file output-dir "a_test.cs")
            stdout (with-out-str
                     (io/make-parents test-file)
                     (spit test-file "public class A {}")
                     (try
                       (with-redefs [clojure.java.shell/sh (fn [& _]
                                                             {:exit 0 :out "ok\n" :err "warnings\n"})]
                         (binding [*err* *out*]
                           (fw/run-specs {:framework :csharp/xunit
                                          :output-dir (str output-dir)
                                          :_used-nses ['gherclj.frameworks.csharp.xunit-spec]})))
                       (finally
                         (.delete test-file)
                         (.delete (io/file output-dir "gherclj.generated.csproj"))
                         (.delete output-dir)
                         (.delete (io/file tmp-dir)))))]
        (should (str/includes? stdout "ok\n"))
        (should (str/includes? stdout "warnings\n"))))))
