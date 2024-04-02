package pkg

import (
	"fmt"
	"github.com/alecthomas/assert/v2"
	"github.com/goccy/go-yaml"
	"strings"
	"testing"
)

func TestGetKeyAndDataMap(t *testing.T) {
	yml := `
key1:
  key2: value2
`
	key, object, err := GetKeyAndData(prepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, "key1", key)
	assert.Equal(t, "value2", object["key2"])
}

func TestGetKeyAndDataString(t *testing.T) {
	yml := `
key1: string1
`
	key, object, err := GetKeyAndData(prepareTestData(t, yml))

	assert.Error(t, err)
	assert.Zero(t, key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataInt(t *testing.T) {
	yml := `
key1: 12
`
	key, object, err := GetKeyAndData(prepareTestData(t, yml))

	assert.Error(t, err)
	assert.Zero(t, key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataBool(t *testing.T) {
	yml := `
key1: true
`
	key, object, err := GetKeyAndData(prepareTestData(t, yml))

	assert.Error(t, err)
	assert.Zero(t, key)
	assert.Zero(t, object)
}

func TestGetKeyAndDataNone(t *testing.T) {
	yml := `
key1:
`
	key, object, err := GetKeyAndData(prepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, "key1", key)
	assert.Zero(t, object)
}

func TestGetAsStringListEmpty(t *testing.T) {
	yml := `
key1:
`
	_, err := GetAsStringList("key1", prepareTestData(t, yml))
	assert.Error(t, err)
}

func TestGetAsStringListString(t *testing.T) {
	yml := `
key1: string1
`
	list, err := GetAsStringList("key1", prepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, []string{"string1"}, list)
}

func TestGetAsStringListSingleItem(t *testing.T) {
	yml := `
key1: 
  - string1
`
	list, err := GetAsStringList("key1", prepareTestData(t, yml))

	assert.NoError(t, err)
	assert.Equal(t, []string{"string1"}, list)
}

func TestGetAsStringListMultipleItems(t *testing.T) {
	yml := `
key1: 
  - string1
  - string2
`
	list, err := GetAsStringList("key1", prepareTestData(t, yml))

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

func TestSandbox(t *testing.T) {
	yml := `
key1:
  - key2:
	  field1: value1
	  field4: oldvalue4
  - key2:
      name: name1
	  field2: value2
key2:
  - key2:
	  field3: value3
key3:
  - key2:
	  field4: value4
`
	path, err := yaml.PathString("$.key1[*]")
	if err != nil {
		//...
	}
	var authors []interface{}
	if err := path.Read(strings.NewReader(yml), &authors); err != nil {
		//...
	}
	fmt.Println(authors)
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

	data, err := CollectDataByKey("key1", "key2", prepareTestData(t, yml))
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

	data, err := CollectDataByKeyAndName("key1", "key2", "name1", prepareTestData(t, yml))
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

	data, err := CollectDataByKey("key1", "key2", prepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Zero(t, data)
}

func TestGetStringKey(t *testing.T) {
	yml := `
key1: string1
`

	data, err := GetStringKey("key1", prepareTestData(t, yml))
	assert.NoError(t, err)
	assert.Equal(t, "string1", data)
}

func prepareTestData(t *testing.T, yml string) map[string]interface{} {

	data := make(map[string]interface{})
	err := yaml.Unmarshal([]byte(yml), &data)
	assert.NoError(t, err)
	return data
}
