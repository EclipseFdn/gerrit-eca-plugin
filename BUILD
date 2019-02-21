load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "gerrit-cla-plugin",
    srcs = glob(["eclipse-cla/src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: gerrit-cla-plugin",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
