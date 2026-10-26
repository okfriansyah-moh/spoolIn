import {
  existsSync,
  mkdirSync,
  readFileSync,
  writeFileSync,
  rmSync,
} from 'node:fs';
import {
  components,
  fingerprint,
  output,
  xmlAttributes,
  sonarCoverageResult,
  writeSummary,
} from './report.mjs';

const host = process.env.SONAR_HOST_URL ?? 'http://sonarqube:9000';
const token = process.env.SONAR_TOKEN;
const gateName = 'SpoolIn unit coverage 80';
const projectKey = (id) => `spoolin-unit-${id}`;

async function api(path, parameters = {}, method = 'GET') {
  const values = new URLSearchParams(parameters);
  const url = new URL(`/api/${path}`, host);
  if (method === 'GET') url.search = values.toString();
  const response = await fetch(url, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: method === 'POST' ? values : undefined,
    signal: AbortSignal.timeout(30000),
  });
  if (!response.ok)
    throw new Error(`Sonar ${path} returned HTTP ${response.status}`);
  return response.status === 204 ? {} : response.json();
}

function currentSummary() {
  const summary = JSON.parse(readFileSync(`${output}/summary.json`, 'utf8'));
  if (summary.reason)
    throw new Error(`Coverage run incomplete: ${summary.reason}`);
  if (summary.fingerprint !== fingerprint())
    throw new Error(
      'Coverage is stale: source inputs changed. Run make coverage.',
    );
  if (summary.components.length !== components.length)
    throw new Error('Incomplete component results');
  for (const component of components) {
    const result = summary.components.find(
      (item) => item.component === component.id,
    );
    if (
      !(result?.tests?.executed > 0) ||
      result.tests.failures ||
      result.tests.errors ||
      !result.metrics?.lines.total
    ) {
      throw new Error(
        `Missing successful test execution or coverage: ${component.id}`,
      );
    }
  }
  return summary;
}

async function ensureGate() {
  const gates = await api('qualitygates/list');
  let gate = gates.qualitygates.find((item) => item.name === gateName);
  if (!gate) {
    gate = await api('qualitygates/create', { name: gateName }, 'POST');
    for (const metric of ['coverage', 'line_coverage']) {
      await api(
        'qualitygates/create_condition',
        { gateName, metric, op: 'LT', error: '80' },
        'POST',
      );
    }
  }
  const details = await api('qualitygates/show', { name: gateName });
  for (const metric of ['coverage', 'line_coverage']) {
    if (
      !details.conditions.some(
        (item) =>
          item.metric === metric &&
          item.op === 'LT' &&
          Number(item.error) === 80,
      )
    ) {
      throw new Error(`The ${gateName} gate requires overall ${metric} < 80.`);
    }
  }
}

async function ensureProject(component) {
  const key = projectKey(component.id);
  const existing = await api('projects/search', { projects: key });
  if (!existing.components.some((item) => item.key === key)) {
    await api(
      'projects/create',
      {
        project: key,
        name: `SpoolIn unit: ${component.id}`,
        visibility: 'private',
      },
      'POST',
    );
  }
  await api('qualitygates/select', { projectKey: key, gateName }, 'POST');
}

function settings(component, summary) {
  const directory = `${output}/${component.id}`;
  const properties = {
    'sonar.projectKey': projectKey(component.id),
    'sonar.projectName': `SpoolIn unit: ${component.id}`,
    'sonar.projectVersion': `${summary.revision}${summary.dirty ? '-working' : ''}`,
    'sonar.projectBaseDir': '/workspace',
    'sonar.sources': `${component.path}/src${component.kind === 'kover' ? '/main/kotlin' : ''}`,
    'sonar.exclusions':
      '**/generated/**,**/*.test.ts,**/*.fixture.ts,**/*.d.ts',
    'sonar.sourceEncoding': 'UTF-8',
    'sonar.working.directory': `${output}/sonar/work-${component.id}`,
    'sonar.qualitygate.wait': 'true',
    'sonar.qualitygate.timeout': '600',
    'sonar.javascript.node.maxspace': '1024',
  };
  if (component.kind === 'kover') {
    properties['sonar.coverage.jacoco.xmlReportPaths'] =
      `${directory}/report.xml`;
    properties['sonar.java.binaries'] =
      `${component.path}/build/classes/kotlin/main`;
    properties['sonar.junit.reportPaths'] = `${directory}/tests`;
  } else {
    properties['sonar.typescript.tsconfigPaths'] =
      `${component.path}/tsconfig.json`;
    // Svelte remains in the complete HTML report and threshold; Sonar cannot analyze it.
    const records = readFileSync(`${directory}/lcov.info`, 'utf8').split(
      'end_of_record',
    );
    const supported = records
      .filter((record) => /^SF:.*\.ts$/m.test(record))
      .map((record) =>
        record.replace(/^SF:(.+)$/m, (_line, file) => {
          const relative = file.replace(/^\/workspace\//, '');
          const path = relative.startsWith('src/')
            ? `${component.path}/${relative}`
            : relative;
          if (!path.startsWith(`${component.path}/src/`))
            throw new Error(`LCOV path outside ${component.id}: ${file}`);
          return `SF:${path}`;
        }),
      );
    if (!supported.length)
      throw new Error(`No supported LCOV source records for ${component.id}`);
    writeFileSync(
      `${directory}/sonar-lcov.info`,
      `${supported.join('end_of_record')}end_of_record\n`,
    );
    properties['sonar.javascript.lcov.reportPaths'] =
      `${directory}/sonar-lcov.info`;
  }
  return (
    Object.entries(properties)
      .map(([key, value]) => `${key}=${value}`)
      .join('\n') + '\n'
  );
}

async function prepare(summary) {
  await ensureGate();
  rmSync(`${output}/sonar`, { recursive: true, force: true });
  rmSync(`${output}/sonar-results.json`, { force: true });
  mkdirSync(`${output}/sonar`, { recursive: true });
  for (const component of components) {
    await ensureProject(component);
    writeFileSync(
      `${output}/sonar/${component.id}.properties`,
      settings(component, summary),
    );
  }
}

async function verifyKoverMappings(component) {
  const response = await api('measures/component_tree', {
    component: projectKey(component.id),
    metricKeys: 'lines_to_cover,uncovered_lines',
    qualifiers: 'FIL',
    ps: '500',
  });
  const files = new Map(response.components.map((file) => [file.path, file]));
  if (response.paging.total > files.size)
    throw new Error(
      'Source mapping verification needs additional result pages',
    );
  const xml = readFileSync(`${output}/${component.id}/report.xml`, 'utf8');
  let mapped = 0;
  for (const packageMatch of xml.matchAll(
    /<package\b([^>]+)>([\s\S]*?)<\/package>/g,
  )) {
    const packageName = xmlAttributes(packageMatch[1]).name;
    for (const source of packageMatch[2].matchAll(
      /<sourcefile\b([^>]+)>([\s\S]*?)<\/sourcefile>/g,
    )) {
      const name = xmlAttributes(source[1]).name;
      const path = `${component.path}/src/main/kotlin/${packageName}/${name}`;
      const file = files.get(path);
      if (!existsSync(path) || !file)
        throw new Error(`Kover source is unresolved in Sonar: ${path}`);
      const expectedCovered = [...source[2].matchAll(/<counter\b([^>]+)\/>/g)]
        .map((match) => xmlAttributes(match[1]))
        .find((counter) => counter.type === 'LINE')?.covered;
      const measures = Object.fromEntries(
        file.measures.map((item) => [item.metric, Number(item.value)]),
      );
      if (
        Number(expectedCovered) > 0 &&
        measures.lines_to_cover > 0 &&
        !(measures.lines_to_cover - measures.uncovered_lines > 0)
      )
        throw new Error(
          `Covered Kover source has no imported hits in Sonar: ${path}`,
        );
      mapped++;
    }
  }
  if (!mapped) throw new Error('No Kover source mappings were verified');
  return mapped;
}

async function verifyComponent(component, summary) {
  const task = Object.fromEntries(
    readFileSync(`${output}/sonar/work-${component.id}/report-task.txt`, 'utf8')
      .trim()
      .split('\n')
      .map((line) => line.split(/=(.*)/s).slice(0, 2)),
  );
  const execution = await api('ce/task', { id: task.ceTaskId });
  if (execution.task.status !== 'SUCCESS')
    throw new Error('Analysis did not complete successfully');
  const gate = await api('qualitygates/project_status', {
    analysisId: execution.task.analysisId,
  });
  const measures = await api('measures/component', {
    component: projectKey(component.id),
    metricKeys:
      'coverage,line_coverage,lines_to_cover,uncovered_lines,conditions_to_cover,uncovered_conditions,security_issues,reliability_issues,duplicated_lines,duplicated_blocks',
  });
  const metrics = Object.fromEntries(
    measures.component.measures.map((item) => [
      item.metric,
      item.metric.endsWith('_issues') ? JSON.parse(item.value).total : Number(item.value),
    ]),
  );
  const expected = summary.components.find(
    (item) => item.component === component.id,
  ).metrics.lines;
  const result = {
    component: component.id,
    analysisId: execution.task.analysisId,
    gate: gate.projectStatus.status,
    expected,
    ...sonarCoverageResult(metrics, gate.projectStatus.status, expected),
  };
  if (!(result.covered > 0))
    throw new Error('Sonar has no covered lines; check report source mappings');
  if (component.kind === 'kover')
    result.sourceMappings = await verifyKoverMappings(component);

  return result;
}

async function verify(summary) {
  const results = [];
  for (const component of components) {
    try {
      results.push(await verifyComponent(component, summary));
    } catch (error) {
      results.push({
        component: component.id,
        status: 'failed',
        reason: error.message,
      });
    }
  }
  const publication = {
    revision: summary.revision,
    fingerprint: summary.fingerprint,
    publishedAt: new Date().toISOString(),
    components: results,
  };
  writeFileSync(
    `${output}/sonar-results.json`,
    JSON.stringify(publication, null, 2) + '\n',
  );
  writeSummary(summary, publication);

  for (const result of results)
    console.log(
      `${result.component}: Sonar ${result.status} — coverage ${result.coverage ?? '?'}%, lines ${result.lines ?? '?'}%${result.reason ? ` — ${result.reason}` : ''}`,
    );
  process.exitCode = results.every((item) => item.status === 'passed') ? 0 : 1;
}

try {
  if (!token) throw new Error('SONAR_TOKEN is required');
  const summary = currentSummary();
  if (process.argv[2] === 'prepare') await prepare(summary);
  else if (process.argv[2] === 'verify') await verify(summary);
  else throw new Error('Usage: sonar.mjs {prepare|verify}');
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
