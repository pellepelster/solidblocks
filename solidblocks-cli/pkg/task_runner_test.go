package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestParseTaskRunner(t *testing.T) {
	yml := `
shell:
  command: some_command
`
	taskRunner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, taskRunner)
}

func TestParseTaskRunnerVariables(t *testing.T) {
	yml := `
shell:
  command: some_command
`

	taskRunner, err := ParseTaskRunner(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.NotZero(t, taskRunner)
}
