"""Export the actual unittest outcomes as JSON counts and JUnit cases."""

import unittest
import xml.etree.ElementTree as ET


class ObservedResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.started_ids = set()

    def startTest(self, test):
        self.started_ids.add(test.id())
        super().startTest(test)


def test_list(suite):
    result = []
    for item in suite:
        result.extend(test_list(item) if isinstance(item, unittest.TestSuite) else [item])
    return result


def parent_id(test):
    return getattr(test, "test_case", test).id()


def write_junit(path, tests, result, suite_name="T001 isolated harness"):
    failures = {parent_id(test): detail for test, detail in result.failures}
    errors = {parent_id(test): detail for test, detail in result.errors}
    skipped = {parent_id(test): reason for test, reason in result.skipped}
    skipped.update({parent_id(test): "Expected failure remains unexecuted acceptance" for test, _ in result.expectedFailures})
    failures.update({parent_id(test): "Unexpected success" for test in result.unexpectedSuccesses})
    skipped.update({test.id(): "Not executed" for test in tests if test.id() not in result.started_ids})
    suite = ET.Element("testsuite", name=suite_name, tests=str(len(tests)),
                       failures=str(len(failures)), errors=str(len(errors)), skipped=str(len(skipped)))
    for test in tests:
        identity = test.id()
        classname, name = identity.rsplit(".", 1)
        case = ET.SubElement(suite, "testcase", classname=classname, name=name)
        for kind, issues in (("failure", failures), ("error", errors), ("skipped", skipped)):
            if identity in issues:
                ET.SubElement(case, kind).text = issues[identity]
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(suite).write(path, encoding="utf-8", xml_declaration=True)
    return {"selected": len(tests), "executed": len(result.started_ids - skipped.keys()),
            "failed": len(failures.keys() | errors.keys())}
