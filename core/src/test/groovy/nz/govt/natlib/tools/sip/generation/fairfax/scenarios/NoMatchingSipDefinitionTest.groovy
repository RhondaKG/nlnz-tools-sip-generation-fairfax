package nz.govt.natlib.tools.sip.generation.fairfax.scenarios

import groovy.util.logging.Slf4j
import nz.govt.natlib.tools.sip.IEEntityType
import nz.govt.natlib.tools.sip.extraction.SipXmlExtractor
import nz.govt.natlib.tools.sip.generation.fairfax.FairfaxFilesProcessor
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper
import nz.govt.natlib.tools.sip.generation.fairfax.TestHelper.TestMethodState
import nz.govt.natlib.tools.sip.state.SipProcessingExceptionReasonType
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

import java.nio.file.Path

import static org.hamcrest.core.Is.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

/**
 * Tests the {@code no-matching-sip-definition} scenario.
 *
 * Note that this test is complicated by the files either being part of a directory structure or in a resource file (jar),
 * so the {@link TestHelper} class is used to handle both scenarios. In real-life processing the files would be on the
 * filesystem and not in a resource. We explicitly use only filesystem files in
 * {@link #correctlyAssembleSipFromFilesOnFilesystem} (as an example to script writers), but this unit test is
 * ignored for builds.
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
class NoMatchingSipDefinitionTest {
    // TODO Make this processing simpler
    // - given a starting folder
    // - and a set of selection criteria
    // - create SIPs for the given files
    static String ID_COLUMN_NAME = "MMSID"

    static final String RESOURCES_FOLDER = "ingestion-files-tests/scenario-no-matching-sip-definition"
    static final String IMPORT_PARAMETERS_FILENAME = "test-fairfax-import-parameters.json"

    TestMethodState testMethodState

    @Before
    void setup() {
        testMethodState = new TestMethodState(ID_COLUMN_NAME, RESOURCES_FOLDER, IMPORT_PARAMETERS_FILENAME)
    }

    /**
     * Note to developers: Ensure that this is exactly the same test as {@link #correctlyAssembleSipFromFiles()}, except
     * that this test only reads from the file system, not a resource file.
     *
     * This test should use the local filesystem when running from within an IDE.
     *
     * This test then becomes a starting point for scripts that create and process SIPs.
     */
    @Test
    // TODO Ignore this test before making a code commit
    @Ignore
    void correctlyAssembleSipFromFilesOnFilesystem() {
        boolean forLocalFilesystem = true
        TestHelper.initializeTestMethod(testMethodState, "NoMatchingSipDefinitionTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<File> filesForProcessing = TestHelper.getFilesForProcessingFromFileSystem(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    @Test
    void correctlyAssembleSipFromFiles() {
        boolean forLocalFilesystem = false
        TestHelper.initializeTestMethod(testMethodState, "NoMatchingSipDefinitionTest-", forLocalFilesystem)

        // TODO A more complicated pattern -- date and other masks?
        boolean isRegexNotGlob = true
        boolean matchFilenameOnly = true
        boolean sortFiles = true
        List<File> filesForProcessing = TestHelper.getFilesForProcessingFromResource(isRegexNotGlob, matchFilenameOnly,
                sortFiles, testMethodState.resourcePath, testMethodState.localPath, ".*?\\.[pP]{1}[dD]{1}[fF]{1}")

        processFiles(filesForProcessing)
    }

    void processFiles(List<File> filesForProcessing) {
        String sipAsXml = FairfaxFilesProcessor.processCollectedFiles(testMethodState.sipProcessingState,
                testMethodState.fairfaxSpreadsheet, filesForProcessing)

        log.info("START SipProcessingState:")
        log.info(testMethodState.sipProcessingState.toString())
        log.info("END SipProcessingState")

        int expectedNumberOfFilesProcessed = 10
        assertThat("${expectedNumberOfFilesProcessed} files should have been processed",
                testMethodState.sipProcessingState.totalFilesProcessed, is(expectedNumberOfFilesProcessed))
        int expectedNumberOfValidFiles = 0
        assertThat("${expectedNumberOfValidFiles} files should have been processed",
                testMethodState.sipProcessingState.validFiles.size(), is(expectedNumberOfValidFiles))
        int expectedNumberOfInvalidFiles = 10
        assertThat("${expectedNumberOfInvalidFiles} files should have been processed",
                testMethodState.sipProcessingState.invalidFiles.size(), is(expectedNumberOfInvalidFiles))
        assertThat("Invalid file is 'TSTPBX-20181123-001.pdf'",
                testMethodState.sipProcessingState.invalidFiles.first().getName(), is("TSTPBX-20181123-001.pdf"))
        int expectedNumberOfUnrecognizedFiles = 0
        assertThat("${expectedNumberOfUnrecognizedFiles} files should have been processed",
                testMethodState.sipProcessingState.unrecognizedFiles.size(), is(expectedNumberOfUnrecognizedFiles))

        log.info("SIP validation")
        sipConstructedCorrectly(sipAsXml)
        log.info("ENDING SIP validation")
        log.info("Process output path=${testMethodState.processOutputInterceptor.path}")
        Path processingStateFilePath = testMethodState.sipProcessingState.toTempFile()
        log.info("sipProcessingState file path=${processingStateFilePath}")
        testMethodState.processOutputInterceptor.stopAndClose()
        // In a normal processing script, the processed files, the processing output and the sipProcessingState file
        // would be moved/copied to a processing completed directory based on the processing state.
    }

    void sipConstructedCorrectly(String sipXml) {
        SipXmlExtractor sipForValidation = new SipXmlExtractor(sipXml)

        assertTrue("SipXmlExtractor has content", sipForValidation.xml.length() > 0)

        assertTrue("SipProcessingState is complete", testMethodState.sipProcessingState.isComplete())
        TestHelper.assertExpectedExceptionReason(testMethodState.sipProcessingState, SipProcessingExceptionReasonType.NO_MATCHING_SIP_DEFINITION)

        TestHelper.assertExpectedSipMetadataValues(sipForValidation, "UNKNOWN_TITLE", 2038, 12, 31,
                IEEntityType.UNKNOWN, "UNKNOWN_OBJECT_IDENTIFIER_TYPE", "UNKNOWN_OBJECT_IDENTIFIER_VALUE",
                "UNKNOWN_POLICY_ID", "UNKNOWN_PRESERVATION_TYPE", "UNKNOWN_USAGE_TYPE", true, 1)

        TestHelper.assertExpectedSipFileValues(sipForValidation, 1, "TSTPBX-20181123-001.pdf", "TSTPBX-20181123-001.pdf",
                11438L, "MD5", "b8b673eeaa076ff19501318a27f85e9c", "001", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 2, "TSTPBX-20181123-002.pdf", "TSTPBX-20181123-002.pdf",
                11437L, "MD5", "df39cff17991188d9994ff94bddf3985", "002", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 3, "TSTPBX-20181123-003with-a-qualifier.pdf", "TSTPBX-20181123-003with-a-qualifier.pdf",
                11657L, "MD5", "1533ab07ff8620fffaec83a2afd92170", "003", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 4, "TSTPBX-20181123-004.pdf", "TSTPBX-20181123-004.pdf",
                11554L, "MD5", "857326c06870577255acd4b21e1a64d7", "004", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 5, "TSTPBX-20181123-005.pdf", "TSTPBX-20181123-005.pdf",
                11605L, "MD5", "02e254147945f60a6a2be1c35ae0689e", "005", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 6, "TSTPBX-20181123-006.pdf", "TSTPBX-20181123-006.pdf",
                11430L, "MD5", "6b932154c4b004a2507d73dc3aaf0736", "006", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 7, "TSTPBX-20181123-007.pdf", "TSTPBX-20181123-007.pdf",
                11543L, "MD5", "a7ceb9001aab17e78cfaf1559f130071", "007", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 8, "TSTPBX-20181123-008.pdf", "TSTPBX-20181123-008.pdf",
                11436L, "MD5", "449dc86bd38979d10c8fb6c3b375a467", "008", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 9, "TSTPBX-20181123-009.pdf", "TSTPBX-20181123-009.pdf",
                11612L, "MD5", "fee5322aa8d3c7a4fe7adeba7953e071", "009", "application/pdf")

        TestHelper.assertExpectedSipFileValues(sipForValidation, 10, "TSTPBX-20181123-010.pdf", "TSTPBX-20181123-010.pdf",
                11440L, "MD5", "f621c3081711e895d8fa3d2dd5e49ffa", "010", "application/pdf")
    }

}