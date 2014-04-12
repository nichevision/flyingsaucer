package com.github.danfickle.flyingsaucer.swing.test;

import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.swing.BoxRenderer;
import org.xhtmlrenderer.util.IOUtil;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * ReferenceComparison runs a comparison of rendering a set of source XHTML files against a 
 */
public class ReferenceComparison {
    private final int width;
    private final boolean isVerbose;
    private static final String LINE_SEPARATOR = "\n";

    public static void main(final String[] args) throws IOException {
        // TODO: check args
        final ReferenceComparison rc = new ReferenceComparison(1024, false);
        final File source = new File(args[0]);
        final File reference = new File(args[1]);
        final File failed = new File(args[2]);
        rc.compareDirectory(source, reference, failed);
    }

    /**
     * Initializes (does not launch) the reference comparison.
     *
     * @param width width at which pages should be rendered
     * @param verbose
     */
    public ReferenceComparison(final int width, final boolean verbose) {
        this.width = width;
        this.isVerbose = verbose;
    }

    public void compareDirectory(final File sourceDirectory, final File referenceDir, final File failedDirectory) throws IOException {
        checkDirectories(sourceDirectory, referenceDir, failedDirectory);
        log("Starting comparison using width " + width);
        IOUtil.deleteAllFiles(failedDirectory);

        final boolean wasEnabled = enableLogging(false);
        try {
            final Iterator<File> fileIt = listSourceFiles(sourceDirectory);
            final CompareStatistics stats = new CompareStatistics();
            while (fileIt.hasNext()) {
                final File file = (File) fileIt.next();
                try {
                    compareFile(file, referenceDir, failedDirectory, stats);
                } catch (final IOException e) {
                    stats.failedIOException(e);
                }
            }
            stats.report();
        } finally {
            enableLogging(wasEnabled);
        }
    }

    private boolean enableLogging(final boolean isEnabled) {
        final String prop = "xr.util-logging.loggingEnabled";
        final boolean orgVal = Boolean.valueOf(System.getProperty(prop)).booleanValue();
        System.setProperty(prop, Boolean.valueOf(isEnabled).toString());
        return orgVal;
    }

    private void checkDirectories(final File sourceDirectory, final File referenceDir, final File failedDirectory) {
        if (!sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("Source dir. doesn't exist, or not a directory: " + sourceDirectory);
        }
        if (!referenceDir.exists() || !referenceDir.isDirectory()) {
            throw new IllegalArgumentException("Reference dir. doesn't exist, or not a directory: " + referenceDir);
        }
        if (failedDirectory.exists() && !failedDirectory.isDirectory()) {
            throw new IllegalArgumentException("Need directory for failed matches, not a directory: " + failedDirectory);
        } else if (!failedDirectory.exists()) {
            if (!failedDirectory.mkdirs()) {
                throw new RuntimeException(
                        "Could not create directory path (.mkdirs failed without an exception) " +
                                failedDirectory.getAbsolutePath()
                );
            }
        }
    }

    private boolean verbose() {
        return isVerbose;
    }

    private Iterator<File> listSourceFiles(final File sourceDirectory) {
        return Arrays.asList(
                sourceDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(final File file, final String s) {
                        return Regress.EXTENSIONS.contains(s.substring(s.lastIndexOf(".") + 1));
                    }
                })
        ).iterator();
    }

    public void compareFile(final File source, final File referenceDir, final File failedDirectory, final CompareStatistics stat) throws IOException {
        log("Comparing " + source.getPath());
        stat.checking(source);
        // TODO: reuse code from Regress
        final BoxRenderer renderer = new BoxRenderer(source, width);
        Box box;
        try {
            log("rendering");
            box = renderer.render();
            log("rendered");
        } catch (final Exception e) {
            e.printStackTrace();
            stat.failedToRender(e);
            storeFailed(failedDirectory, source);
            log("Could not render input file, skipping: " + source + " err: " + e.getMessage());
            return;
        }
        final LayoutContext layoutContext = renderer.getLayoutContext();
        final String inputFileName = source.getName();
        final String refRendered = trimTrailingLS(readReference(referenceDir, inputFileName, Regress.RENDER_SFX));
        final String rendered = trimTrailingLS(box.dump(layoutContext, "", Box.DUMP_RENDER));
        if (!compareLines(refRendered, rendered, stat)) {
            storeFailed(failedDirectory, new File(referenceDir, inputFileName), Regress.RENDER_SFX, rendered);
        }

        final String refLaidOut = trimTrailingLS(readReference(referenceDir, inputFileName, Regress.LAYOUT_SFX));
        final String laidOut = trimTrailingLS(box.dump(layoutContext, "", Box.DUMP_LAYOUT));
        if (!compareLines(refLaidOut, laidOut, stat)) {
            storeFailed(failedDirectory, new File(referenceDir, inputFileName), Regress.LAYOUT_SFX, laidOut);
        }
    }

    private String trimTrailingLS(String s) {
        if (s.endsWith(LINE_SEPARATOR)) {
            s = s.substring(0, s.length() - LINE_SEPARATOR.length());
        }
        return s;
    }

    private void storeFailed(final File failedDirectory, final File refFile, final String suffix, final String compareTo) {
        copyToFailed(failedDirectory, refFile, "");
        copyToFailed(failedDirectory, refFile, Regress.PNG_SFX);
        copyToFailed(failedDirectory, refFile, suffix);

        OutputStreamWriter fw = null;
        try {
            fw = new OutputStreamWriter(new FileOutputStream(new File(failedDirectory, refFile.getName() + ".err" + suffix)), "UTF-8");
            final BufferedWriter bw = new BufferedWriter(fw);
            try {
                bw.write(compareTo);
                bw.flush();
            } catch (final IOException e) {
                throw new RuntimeException("unexpected IO exception on writing 'failed' info for test.", e);
            } finally {
                try {
                    bw.close();
                } catch (final IOException e) {
                    // swallow
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException("unexpected IO exception on writing 'failed' info for test.", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (final IOException e) {
                    // swallow
                }
            }
        }
    }

    private void copyToFailed(final File failedDirectory, final File refFile, final String suffix) {
        File source = new File(failedDirectory, refFile.getName() + suffix);
        if (!source.exists()) {
            source = new File(refFile.getAbsoluteFile().getParentFile(), refFile.getName() + suffix);
            try {
                IOUtil.copyFile(source, failedDirectory);
            } catch (final IOException e) {
                System.err.println("Failed to copy file (reference) " + source + " to failed directory, err " + e.getMessage());
            }

        }
    }

    private boolean compareLines(final String refText, final String text, final CompareStatistics statistics) throws IOException {
        log("running comparison");
        final LineNumberReader lnrRef = new LineNumberReader(new StringReader(refText));
        final LineNumberReader lnrOther = new LineNumberReader(new StringReader(text));
        String lineRef;
        String lineOther;
        while ((lineRef = lnrRef.readLine()) != null) {
            lineOther = lnrOther.readLine();
            if (lineOther == null) {
                statistics.failedRefIsLonger();
                return false;
            }
            if (!lineRef.equals(lineOther)) {
                statistics.failedDontMatch(lineRef, lineOther);
                return false;
            }
        }
        if (lnrOther.readLine() != null) {
            statistics.failedOtherIsLonger();
            return false;
        }
        return true;
    }

    private void storeFailed(final File failedDirectory, final File sourceFile) {
        try {
            IOUtil.copyFile(sourceFile, failedDirectory);
        } catch (final IOException e) {
            System.err.println("Failed to copy file to failed directory: " + sourceFile + ", err: " + e.getMessage());
        }
    }

    private String readReference(final File referenceDir, final String input, final String sfx) throws IOException {
        BufferedReader rdr = null;
        StringBuffer sb;
        final File f = new File(referenceDir, input + sfx);
        rdr = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            String line;
            sb = new StringBuffer();
            while ((line = rdr.readLine()) != null) {
                sb.append(line);
                sb.append(LINE_SEPARATOR);
            }
        } finally {
            try {
                rdr.close();
            } catch (final IOException e) {
                // swallow
            }
        }
        return sb.toString();
    }

    private void log(final String msg) {
        if (verbose()) {
            System.out.println(msg);
        }
    }

    private static class CompareStatistics {
        private File currentFile;
        private static final Result OK = new ResultOK();
        private final Map<File, Result> files;

        public CompareStatistics() {
            files = new HashMap<>();
        }

        public void failedToRender(final Exception e) {
            files.put(currentFile, new RenderFailed(e));
        }

        public void failedRefIsLonger() {
            files.put(currentFile, new RefIsLonger());
        }

        public void failedDontMatch(final String lineRef, final String lineOther) {
            files.put(currentFile, new LineMismatch(lineRef, lineOther));
        }

        public void failedOtherIsLonger() {
            files.put(currentFile, new OtherIsLonger());
        }

        public void failedIOException(final IOException e) {
            files.put(currentFile, new FailedIO(e));
        }

        @SuppressWarnings("unused")
		public boolean failed() {
            return files.get(currentFile) instanceof FailedResult;
        }

        public void checking(final File source) {
            currentFile = source;
            files.put(currentFile, OK);
        }

        @SuppressWarnings("unused")
		public boolean succeeded() {
            return files.get(currentFile) instanceof ResultOK;
        }

        public void report() {
            int failed = 0;
            for (final File file : files.keySet()) {
                final Result result = (Result) files.get(file);

                if (result instanceof FailedResult) {
                    failed++;
                    System.out.println(result.describe(file));
                }
            }
            System.out.println("Checked " + files.keySet().size() + " files, " + (failed > 0 ? failed + " failed." : "all OK."));
        }

        private static class RenderFailed implements Result {
            private final Exception exception;

            public RenderFailed(final Exception exception) {
                this.exception = exception;
            }

            public String describe(final File file) {
                return "FAIL: Render operation threw exception for " + file.getName() + ", err " + exception.getMessage();
            }
        }

        private static class RefIsLonger implements FailedResult {
            public String describe(final File file) {
                return "FAIL: reference is longer (more lines): " + file.getName();
            }
        }

        private static class LineMismatch implements FailedResult {
            private final String lineRef;
            private final String lineOther;

            public LineMismatch(final String lineRef, final String lineOther) {
                this.lineRef = lineRef;
                this.lineOther = lineOther;
            }

            public String describe(final File file) {
                return "FAIL: line content doesn't match for " + file.getName() + LINE_SEPARATOR + "ref: " + lineRef + LINE_SEPARATOR + "other: " + lineOther;
            }
        }

        private static class OtherIsLonger implements FailedResult {
            public String describe(final File file) {
                return "FAIL: new rendered output is longer (more lines): " +  file.getName();
            }
        }

        private static class FailedIO implements FailedResult {
            private final IOException exception;

            public FailedIO(final IOException e) {
                this.exception = e;
            }

            public String describe(final File file) {
                return "FAIL: IOException when comparing: " + file + " (err: " + exception.getMessage();
            }
        }

        private interface Result {
            String describe(File file);
        }
        private interface FailedResult extends Result {}

        private static class ResultOK implements Result {
            public String describe(final File file) {
                return "OK: " + file.getName();
            }
        }
    }

}
