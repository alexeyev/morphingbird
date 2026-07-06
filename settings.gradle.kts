rootProject.name = "morphingbird"

include("core")
include("idea-plugin")
// The plugin subproject lives in idea-plugin/ but is named "morphingbird" so
// the built distribution is morphingbird-<version>.zip with a morphingbird/
// content root (what the IDE extracts into its plugins/ directory) instead of
// a collision-prone generic "idea-plugin".
project(":idea-plugin").name = "morphingbird"
