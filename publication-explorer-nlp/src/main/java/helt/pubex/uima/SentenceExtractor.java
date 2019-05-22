package helt.pubex.uima;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.io.pdf.PdfReader;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class SentenceExtractor {
    private final Path input;

    public SentenceExtractor(Path input) {
        this.input = Objects.requireNonNull(input);

    }

    public Stream<String> run() throws ResourceInitializationException {

        JCasIterable jCasIterable = SimplePipeline.iteratePipeline(
                createReaderDescription(PdfReader.class,
                        ResourceCollectionReaderBase.PARAM_LANGUAGE, "en",
                        ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, input.toString(),
                        ResourceCollectionReaderBase.PARAM_PATTERNS, "[+]/*.pdf"),
                createEngineDescription(OpenNlpSegmenter.class)
        );

        Stream.Builder<String> builder = Stream.builder();
        for (JCas cas : jCasIterable) {
            Collection<Sentence> select = JCasUtil.select(cas, Sentence.class);

            select.stream()
                    .map(Sentence::getCoveredText)
                    .map(x -> x.replaceAll("\\s", " "))
                    .forEach(builder::add);
        }

        return builder.build();
    }
}
