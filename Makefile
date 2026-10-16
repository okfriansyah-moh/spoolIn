.PHONY: test-unit test-integration
export TASK

# T037 owns start, stop, restart, status and logs (§11.3).
# TASK=T001 selects a task; omitted TASK selects the entire declared unit map.
test-unit:
	@if [ -n "$$TASK" ]; then bash ops/bin/test-unit.sh --task "$$TASK"; else bash ops/bin/test-unit.sh; fi

test-integration:
	@if [ -n "$$TASK" ]; then bash ops/bin/test-integration.sh --task "$$TASK"; else bash ops/bin/test-integration.sh; fi
