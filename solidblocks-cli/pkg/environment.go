package pkg

import (
	"fmt"
	"github.com/charmbracelet/log"
	"os"
)

type EnvironmentVariable struct {
	Name  string
	Value string
}

func (variable EnvironmentVariable) LogPlanText() string {
	if len(variable.Value) > 0 {
		return fmt.Sprintf("variable '%s' will be set to '%s'", variable.Name, variable.Value)
	} else {
		return fmt.Sprintf("value for variable '%s' will inherited from the calling environment", variable.Name)
	}
}

func GetEnvironment(workflow Workflow) map[string]string {

	result := make(map[string]string)
	for _, environment := range workflow.Environment {

		if len(environment.Value) > 0 {
			result[environment.Name] = environment.Value
		} else {
			parent := os.Getenv(environment.Name)

			if len(parent) == 0 {
				log.Warnf("environment variable '%s' defined as inherited from parent but is not set", environment.Name)
			}
			result[environment.Name] = parent
		}
	}

	return result
}
