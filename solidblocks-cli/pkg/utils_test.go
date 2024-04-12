package pkg

import (
	"github.com/alecthomas/assert/v2"
	"github.com/goccy/go-yaml"
	"testing"
)

func TestGetKeyAndDataMap(t *testing.T) {
	yml := `
key1:
  key2: value2
`
	key, object, err := GetKeyAndData(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, "key1", key)
	assert.Equal(t, "value2", object["key2"])
}

func TestGetDataForKey(t *testing.T) {
	yml := `
key1:
  key2: value2
key3:
  key4: value4
`
	data, err := GetDataForKey("key1", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Equal(t, "value2", data["key2"])

	data, err = GetDataForKey("key3", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Equal(t, "value4", data["key4"])
}

func TestGetDataForKeyMissingKey(t *testing.T) {
	yml := `
key1:
  key2: value2
key3:
  key4: value4
`
	data, err := GetDataForKey("key5", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Zero(t, data)
}

func TestGetKeyAndDataString(t *testing.T) {
	yml := `
key1: string1
`
	key, object, err := GetKeyAndData(PrepareTestData(t, yml))

	assert.Error(t, err)
	assert.Equal(t, "key1", key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataInt(t *testing.T) {
	yml := `
key1: 12
`
	key, object, err := GetKeyAndData(PrepareTestData(t, yml))

	assert.Error(t, err)
	assert.Equal(t, "key1", key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataBool(t *testing.T) {
	yml := `
key1: true
`
	key, object, err := GetKeyAndData(PrepareTestData(t, yml))

	assert.Error(t, err)
	assert.Equal(t, "key1", key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataNone(t *testing.T) {
	yml := `
key1:
`
	key, object, err := GetKeyAndData(PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, "key1", key)
	assert.Zero(t, object)
}

func TestGetAsStringListEmpty(t *testing.T) {
	yml := `
key1:
`
	_, err := GetAsStringList("key1", PrepareTestData(t, yml))
	assert.Error(t, err)
}

func TestGetAsStringListString(t *testing.T) {
	yml := `
key1: string1
`
	list, err := GetAsStringList("key1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, []string{"string1"}, list)
}

func TestGetAsStringListSingleItem(t *testing.T) {
	yml := `
key1: 
  - string1
`
	list, err := GetAsStringList("key1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, []string{"string1"}, list)
}

func TestGetAsStringListMultipleItems(t *testing.T) {
	yml := `
key1: 
  - string1
  - string2
`
	list, err := GetAsStringList("key1", PrepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, []string{"string1", "string2"}, list)
}

func TestMergeMap(t *testing.T) {
	base := make(map[string]interface{})
	base["string1"] = "value1"
	base["string2"] = "value2"

	baseMap1 := make(map[string]interface{})
	baseMap1["string4"] = "value4"
	base["baseMap1"] = baseMap1

	baseMap2 := make(map[string]interface{})
	baseMap2["string5"] = "value5"
	base["map2"] = baseMap2

	override := make(map[string]interface{})
	override["string2"] = "value22"
	overrideMap2 := make(map[string]interface{})
	overrideMap2["string5"] = "value55"
	override["map2"] = overrideMap2

	result := MergeMaps(base, override)

	assert.NotZero(t, result)
	assert.Equal(t, "value22", result["string2"])
	assert.Equal(t, "value55", result["map2"].(map[string]interface{})["string5"])
}

func TestCollectDataByKey(t *testing.T) {
	yml := `
key1:
  - key2:
      field1: value1
      field2: value2
  - key2:
      name: name1
      field3: value3
key2:
  - key2:
      field4: value4
`

	data, err := CollectDataByKey("key1", "key2", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.NotZero(t, data)
	assert.Equal(t, "value1", data["field1"])
}

func TestCollectDataByKeyAndName(t *testing.T) {
	yml := `
key1:
  - key2:
      field1: value1
      field2: value2
  - key2:
      name: name1
      field3: value3
key2:
  - key2:
      field4: value4
`

	data, err := CollectDataByKeyAndName("key1", "key2", "name1", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.NotZero(t, data)
	assert.Equal(t, "value3", data["field3"])
}

func TestCollectDataByKeyNoMatch(t *testing.T) {
	yml := `
key2:
  - key2:
      field4: value4
`

	data, err := CollectDataByKey("key1", "key2", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Zero(t, data)
}

func TestGetStringKey(t *testing.T) {
	yml := `
key1: string1
`

	data, err := GetStringByKey("key1", PrepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Equal(t, "string1", data)
}

func TestGetStringKeyInvalid(t *testing.T) {
	yml := `
key1: string1
`

	data, err := GetStringByKey("key2", PrepareTestData(t, yml))
	assert.Error(t, err)
	assert.Zero(t, data)
}

func TestGetOptionalStringKeyInvalid(t *testing.T) {
	yml := `
key1: string1
`

	data := GetOptionalStringByKey("key2", PrepareTestData(t, yml))
	assert.Zero(t, data)
}

func TestGetOptionalStringKey(t *testing.T) {
	yml := `
key1: string1
`

	data := GetOptionalStringByKey("key1", PrepareTestData(t, yml))
	assert.Equal(t, "string1", data)
}

func TestCommandExists(t *testing.T) {
	assert.True(t, CommandExists("echo"))
}

func TestCommandDoesNotExist(t *testing.T) {
	assert.False(t, CommandExists("invalid_command"))
}

func PrepareTestData(t *testing.T, yml string) map[string]interface{} {

	data := make(map[string]interface{})
	err := yaml.Unmarshal([]byte(yml), &data)
	assert.NoError(t, err)
	return data
}
