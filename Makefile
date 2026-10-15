.PHONY: test-unit test-integration

# T039 owns start, stop, restart, status and logs (§11.3).
# TASK=T001 selects a task; omitted TASK selects the entire declared unit map.
test-unit:
	bash ops/bin/test-unit.sh $(if $(TASK),--task $(TASK))

test-integration:
	bash ops/bin/test-integration.sh $(if $(TASK),--task $(TASK))
