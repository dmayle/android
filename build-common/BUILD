load("//tools/base/bazel:bazel.bzl", "iml_module")

iml_module(
    name = "build-common",
    srcs = ["src"],
    iml_files = ["build-common.iml"],
    resources = ["resources"],
    tags = ["managed"],
    test_resources = ["testResources"],
    test_srcs = ["testSrc"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    exports = [
        "//tools/adt/idea/android/lib:jarutils",
        "//tools/base/common:studio.common",
        "//tools/base/ddmlib:studio.ddmlib",
        "//tools/base/device_validator:studio.dvlib",
        "//tools/base/layoutlib-api:studio.layoutlib-api",
        "//tools/base/lint:studio.lint-api",
        "//tools/base/lint:studio.lint-checks",
        "//tools/base/ninepatch:studio.ninepatch",
        "//tools/base/sdk-common:studio.sdk-common",
        "//tools/base/sdklib:studio.sdklib",
    ],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/util[module]",
        "//tools/adt/idea/android/lib:jarutils",
        "//tools/idea/.idea/libraries:NanoXML",
        "//tools/base/common:studio.common[module]",
        "//tools/base/ddmlib:studio.ddmlib[module]",
        "//tools/base/device_validator:studio.dvlib[module]",
        "//tools/base/layoutlib-api:studio.layoutlib-api[module]",
        "//tools/base/lint:studio.lint-api[module]",
        "//tools/base/lint:studio.lint-checks[module]",
        "//tools/base/ninepatch:studio.ninepatch[module]",
        "//tools/base/sdk-common:studio.sdk-common[module]",
        "//tools/base/sdklib:studio.sdklib[module]",
        "//tools/base/testutils:studio.testutils[module, test]",
        "//tools/adt/idea/android-common",
    ],
)
