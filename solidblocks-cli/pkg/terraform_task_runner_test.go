package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestTerraformTaskRunnerParse(t *testing.T) {
	yml := `
shell:
  command: some_command
`
	runner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, runner)
	assert.Zero(t, runner.(ShellTaskRunner).Workdir)
}

func TestTerraformTaskRunnerEmptyWorkDir(t *testing.T) {
	yml := `
shell:
  command: some_command
`
	runner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, runner)
	assert.Zero(t, runner.(ShellTaskRunner).Workdir)
}
