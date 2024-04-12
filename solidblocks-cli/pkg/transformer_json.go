package pkg

import (
	"encoding/json"
	"github.com/PaesslerAG/jsonpath"
	"strings"
)

func TransformJson(input, path string) (string, error) {

	v := interface{}(nil)

	err := json.Unmarshal([]byte(input), &v)
	if err != nil {
		return "", err
	}

	value, err := jsonpath.Get(path, v)
	if err != nil {
		return "", err
	}

	output, err := json.Marshal(value)
	if err != nil {
		return "", err
	}

	return strings.Trim(string(output), "\""), nil
}
