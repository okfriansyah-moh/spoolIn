export SERVICE TASK

.PHONY: start stop restart status logs seed test-unit test-integration

start:
	@bash ops/bin/start.sh
stop:
	@bash ops/bin/stop.sh
restart:
	@bash ops/bin/restart.sh
status:
	@bash ops/bin/status.sh
logs:
	@bash ops/bin/logs.sh "$${SERVICE}"
seed:
	@bash ops/bin/seed.sh
test-unit:
	@bash ops/bin/test-unit.sh
test-integration:
	@bash ops/bin/test-integration.sh

.PHONY: local-setup local-start local-status local-logs local-stop local-test local-reset local-repair-maintainer local-repair-accounts
local-setup local-start local-status local-logs local-stop local-test local-reset local-repair-maintainer local-repair-accounts:
	@python3 ops/local/run.py $(patsubst local-%,%,$@)

.PHONY: local-trust
local-trust:
	@python3 ops/local/trust.py
