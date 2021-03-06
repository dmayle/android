load("//tools/base/bazel:bazel.bzl", "iml_module")

iml_module(
    name = "designer",
    srcs = ["src"],
    iml_files = ["designer.iml"],
    javacopts = [
        "-Xep:MissingCasesInEnumSwitch:WARN",
        "-Xep:IdentityBinaryExpression:WARN",
    ],
    resources = ["resources"],
    tags = ["managed"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea/platform/editor-ui-api[module]",
        "//tools/idea/platform/core-api[module]",
        "//tools/idea/platform/platform-api[module]",
        "//tools/adt/idea/android[module]",
        "//tools/sherpa:sherpa-solver[module]",
        "//tools/adt/idea/sherpa-ui[module]",
        "//tools/idea/xml/xml-psi-api[module]",
        "//tools/idea:platform-impl_and_others[module]",
        "//tools/base/pixelprobe:studio.pixelprobe[module]",
        "//tools/base/annotations:studio.android-annotations[module]",
        "//tools/idea/.idea/libraries:jsr305",
        "//tools/idea/platform/lang-api[module]",
        "//tools/idea/.idea/libraries:layoutlib",
        "//tools/idea/java/java-psi-impl[module]",
        "//tools/idea/java/java-indexing-api[module]",
        "//tools/adt/idea/adt-ui[module]",
        "//tools/analytics-library/protos:analytics-protos[module]",
        "//tools/analytics-library/tracker:analytics-tracker[module]",
        "//tools/analytics-library/shared:analytics-shared[module]",
        "//tools/adt/idea/wizard[module]",
        "//tools/base/flags:studio.flags[module]",
        "//tools/adt/idea/artwork[module]",
    ],
)

iml_module(
    name = "designer-tests",
    iml_files = ["designer-tests.iml"],
    tags = ["managed"],
    test_class = "com.android.tools.idea.DesignerTestSuite",
    test_data = [
        "//prebuilts/studio/jdk",
        "//prebuilts/studio/layoutlib:buildprop",
        "//prebuilts/studio/layoutlib:data/fonts",
        "//prebuilts/studio/layoutlib:data/res",
        "//prebuilts/studio/sdk:build-tools/latest",
        "//prebuilts/studio/sdk:constraint-layout_latest",
        "//prebuilts/studio/sdk:navigation-runtime_latest",
        "//prebuilts/studio/sdk:platforms/latest",
        "//prebuilts/studio/sdk:support_latest",
        "//prebuilts/tools/common/offline-m2:android-gradle-2.2.0",
        "//tools/adt/idea/android/annotations",
        "//tools/adt/idea/android/lib:androidWidgets",
        "//tools/adt/idea/android/lib:sampleData",
        "//tools/adt/idea/android/testData",
        "//tools/adt/idea/designer/testData",
        "//tools/base/build-system:gradle-distrib",
        "//tools/base/bazel:offline_repo_repo.zip",
        "//tools/base/templates",
        "//tools/idea/java/jdkAnnotations",
        "//tools/idea/java/mockJDK-1.7",
    ],
    test_srcs = ["testSrc"],
    test_tags = [
        "slow",
        "cpu:3",
        "block-network",
    ],
    test_timeout = "long",
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/idea:platform-impl_and_others[module]",
        "//tools/idea/.idea/libraries:mockito[test]",
        "//tools/adt/idea/designer[module, test]",
        "//tools/adt/idea/android[module, test]",
        "//tools/idea/xml/xml-psi-api[module, test]",
        "//tools/idea:compiler-impl_and_others[module]",
        "//tools/idea/.idea/libraries:truth[test]",
        "//tools/idea/platform/lang-api[module, test]",
        "//tools/idea/.idea/libraries:layoutlib[test]",
        "//tools/base/testutils:studio.testutils[module, test]",
        "//tools/adt/idea/adt-ui[module, test]",
        "//tools/base/pixelprobe:studio.pixelprobe[module, test]",
        "//tools/adt/idea/sherpa-ui[module, test]",
        "//tools/adt/idea/android-test-framework[module, test]",
        "//tools/analytics-library/tracker:analytics-tracker[module, test]",
        "//tools/analytics-library/protos:analytics-protos[module, test]",
        "//tools/analytics-library/shared:analytics-shared[module, test]",
        "//tools/adt/idea/adt-branding[module, test]",
        "//tools/adt/idea/sdk-updates[module, test]",
        "//tools/base/layoutinspector:studio.layoutinspector[module, test]",
        "//tools/idea/.idea/libraries:Gradle[test]",
        "//tools/base/flags:studio.flags[module]",
        "//tools/adt/idea/artwork[module, test]",
    ],
)
