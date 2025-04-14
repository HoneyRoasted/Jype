package honeyroasted.jypestub.model;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jypestub.model.test.JStubTest;
import honeyroasted.jypestub.model.types.JStubClass;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public record JStubFile(String name, Map<String, JStubClass> classes, Map<String, JStubTest> tests) {

    public static JStubFile of(Collection<JClassType> classTypes) {
        Map<String, JStubClass> classes = new LinkedHashMap<>();
        classTypes.forEach(jct -> {
            String internalName = jct.namespace().location().toInternalName();
            JStubClass stubClass = JStubClass.of(jct);
            classes.put(internalName, stubClass);
        });
        return new JStubFile("generated", classes, new LinkedHashMap<>());
    }

    public Map<String, JStubTest.Result> runTests(JTypeSystem system) {
        Map<String, JStubTest.Result> results = new LinkedHashMap<>();
        this.tests.forEach((name, test) -> results.put(name, test.test(system)));
        return results;
    }

    public String runTestsReport(JTypeSystem system) {
        Map<String, JStubTest.Result> results = this.runTests(system);
        int failCount = results.values().stream().mapToInt(r -> r.result() ? 0 : 1).sum();

        StringBuilder sb = new StringBuilder();
        if (failCount == 0) {
            sb.append("All tests passed!");
        } else {
            sb.append(failCount).append(" tests failed!");
        }
        sb.append("\n\n");

        results.forEach((key, result) -> {
            result.toString(sb, "    ", key + ": ");
            sb.append("\n");
        });

        return sb.toString();
    }

}
