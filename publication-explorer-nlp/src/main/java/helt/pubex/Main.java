package helt.pubex;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpNamedEntityRecognizer;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Writer;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.languagetool.LanguageToolLemmatizer;
import de.tudarmstadt.ukp.dkpro.core.maltparser.MaltParser;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import helt.pubex.dl4j.Dl4jVectorizerService;
import helt.pubex.uima.UimaNlpTopicModellingService;

import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

public class Main {
	public static final Logger LOG = LoggerFactory.getLogger("Main");

	public static void main(String[] args)
			throws UIMAException, IOException, InterruptedException, ExecutionException, URISyntaxException {

		if (args.length == 0) {
			args = new String[] { "lda" };
		}

		Path input = Paths.get("data/input/pdf");
		Path output = Paths.get("data/output/");
		if ("lda".equals(args[0])) {

			nerAndTopicModelingExample(input);
		} else if ("pv".equals(args[0])) {
			mapParagraphVectors(input, output);
		} else {
			// untested
			basicExample();
		}
	}

	private static void mapParagraphVectors(Path input, Path output)
			throws ResourceInitializationException, IOException {
		Dl4jVectorizerService service = new Dl4jVectorizerService(input, output);

		service.extractSentencesFromData();
		service.convertSentencesToVectors();
	}

	private static void nerAndTopicModelingExample(Path input)
			throws IOException, InterruptedException, ExecutionException, UIMAException, URISyntaxException {
		UimaNlpTopicModellingService service = new UimaNlpTopicModellingService();

		LOG.info("unprocessed data in {}", input);
		service.processDirectory(input);
		LOG.info("processed data in {}", service.getOutputDirectory().toString());

	}

	public static void basicExample() throws UIMAException, IOException {
		runPipeline(
				createReaderDescription(TextReader.class, ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
						"data/documents/document.txt", ResourceCollectionReaderBase.PARAM_LANGUAGE, "en"),
				createEngineDescription(OpenNlpSegmenter.class), createEngineDescription(OpenNlpPosTagger.class),
				createEngineDescription(LanguageToolLemmatizer.class), createEngineDescription(MaltParser.class),
				createEngineDescription(CoreNlpNamedEntityRecognizer.class),
				createEngineDescription(Conll2006Writer.class, JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, "."));

	}
}
