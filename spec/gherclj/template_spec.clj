(ns gherclj.template-spec
  (:require [speclj.core :refer :all]
            [gherclj.template :refer :all]))

(describe "Template compiler"

  (context "compile-template"

    (it "compiles a plain string with no captures"
      (let [{:keys [regex bindings]} (compile-template "checking for zombies")]
        (should= "^checking for zombies$" (str regex))
        (should= [] bindings)))

    (it "compiles a string capture"
      (let [{:keys [regex bindings]} (compile-template "a project {slug:string}")]
        (should= 1 (count bindings))
        (should= "slug" (:name (first bindings)))))

    (it "string capture matches greedily bounded by surrounding literal"
      (let [compiled (compile-template "a project {slug:string} with timeout {t:int}")
            result (match-step compiled "a project alpha beta with timeout 300")]
        (should= ["alpha beta" 300] result)))

    (it "string capture at end matches rest of string"
      (let [compiled (compile-template "the value is {val:string}")
            result (match-step compiled "the value is hello world")]
        (should= ["hello world"] result)))

    (it "compiles an int capture"
      (let [{:keys [regex bindings]} (compile-template "timeout is {seconds:int}")]
        (should= "^timeout is (\\d+)$" (str regex))
        (should= [{:name "seconds" :coerce parse-long}] bindings)))

    (it "compiles a float capture"
      (let [{:keys [regex bindings]} (compile-template "ratio is {value:float}")]
        (should= "^ratio is ([\\d.]+)$" (str regex))
        (should= [{:name "value" :coerce parse-double}] bindings)))

    (it "compiles an untyped capture as word match"
      (let [{:keys [regex bindings]} (compile-template "status is {status}")]
        (should= "^status is (\\S+)$" (str regex))
        (should= [{:name "status" :coerce identity}] bindings)))

    (it "compiles multiple captures"
      (let [{:keys [regex bindings]} (compile-template "a project {slug:string} with worker-timeout {timeout:int}")]
        (should= 2 (count bindings))
        (should= "slug" (:name (first bindings)))
        (should= "timeout" (:name (second bindings)))))

    (it "escapes regex special characters in literal text"
      (let [{:keys [regex]} (compile-template "progress is 1/3 (33%)")]
        (should= "^progress is 1/3 \\(33%\\)$" (str regex)))))

  (context "match-step"

    (it "matches and extracts with coercion"
      (let [compiled (compile-template "a project {slug:string} with worker-timeout {timeout:int}")
            result (match-step compiled "a project alpha with worker-timeout 300")]
        (should= ["alpha" 300] result)))

    (it "matches string capture with quotes in text, strips quotes"
      (let [compiled (compile-template "a user {name:string}")
            result (match-step compiled "a user \"alice\"")]
        (should= ["alice"] result)))

    (it "returns nil on no match"
      (let [compiled (compile-template "a project {slug:string}")
            result (match-step compiled "something else entirely")]
        (should-be-nil result)))

    (it "matches plain text with no captures"
      (let [compiled (compile-template "checking for zombies")
            result (match-step compiled "checking for zombies")]
        (should= [] result)))))
