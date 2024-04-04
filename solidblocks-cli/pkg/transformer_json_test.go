package pkg

import (
	"github.com/alecthomas/assert/v2"
	"testing"
)

func TestTransformJsonString(t *testing.T) {

	output, err := TransformJson(`{
		"string": "string1"
		}`, "$.string")

	assert.NoError(t, err)
	assert.Equal(t, "\"string1\"", output)
}

func TestTransformJsonInt(t *testing.T) {

	output, err := TransformJson(`{
		"integer": 1
		}`, "$.integer")

	assert.NoError(t, err)
	assert.Equal(t, "1", output)
}

func TestTransformJsonObject(t *testing.T) {

	output, err := TransformJson(`{
		"object": {
				"string": "string2"
			}
		}`, "$.object")

	assert.NoError(t, err)
	assert.Equal(t, "{\"string\":\"string2\"}", output)
}

func TestTransformJsonObjectString(t *testing.T) {

	output, err := TransformJson(`{
		"object": {
				"string": "string2"
			}
		}`, "$.object.string")

	assert.NoError(t, err)
	assert.Equal(t, "\"string2\"", output)
}
