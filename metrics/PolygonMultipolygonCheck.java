/**
 * 
 */
package eu.diachron.qualitymetrics.accessibility.availability;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import de.unibonn.iai.eis.diachron.semantics.DQM;
import de.unibonn.iai.eis.diachron.semantics.DQMPROB;
import de.unibonn.iai.eis.diachron.semantics.knownvocabs.GEOSPARQL;
import de.unibonn.iai.eis.diachron.technques.probabilistic.ReservoirSampler;
import de.unibonn.iai.eis.luzzu.datatypes.ProblemList;
import de.unibonn.iai.eis.luzzu.semantics.vocabularies.QPRO;
import eu.diachron.qualitymetrics.utilities.AbstractQualityMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Beyza Yaman
 * 
 *
 * 
 * Based on:
 * 
 */
public class PolygonMultipolygonCheck extends AbstractQualityMetric {

	private final Resource METRIC_URI = DQM.MisusedOwlDatatypeOrObjectPropertiesMetric;

	private static Logger logger = LoggerFactory.getLogger(PolygonMultipolygonCheck.class);

//	private static DB mapDb = MapDbFactory.getMapDBAsyncTempFile();
//	protected Set<SerialisableModel> problemList =  MapDbFactory.createHashSet(mapDb, UUID.randomUUID().toString());

	// Sampling of problems - testing for LOD Evaluation
	ReservoirSampler<ProblemReport> problemSampler = new ReservoirSampler<ProblemReport>(50000, false);

	private double misuseDatatypeProperties = 0.0;
	private double misuseObjectProperties = 0.0;
	private double validPredicates = 0.0;
	boolean check3=false;

	/**
	 * This method computes identified a given quad is a misuse owl data type
	 * property or object property.
	 *
	 * @param quad - to be identified
	 *
	 **/
	
	public void compute(Quad quad) {
		logger.debug("Computing : {} ", quad.asTriple().toString());

		Node subject = quad.getSubject();
		Node predicate = quad.getPredicate();
		Node object = quad.getObject();

		String URI="";
		String literalURI= "http://www.opengis.net/ont/geosparql#wktLiteral";
		String greekProp= "http://geo.linkedopendata.gr/coastline_gr/ontology#hasGeometry";
		//boolean check1,check2,check3 = false;


		if ((predicate.getURI().equals(GEOSPARQL.asWKTproperty.getURI()))){  //It was RDF.type.getURI()

			if (object.isLiteral()){
				//String str = "ZZZZL <%= dsn %> AFFF <%= AFG %>";
				String obj= object.toString();
				Pattern patternShape;//(?=.*MULTIPOLYGON)(?=.*MULTIPOLYGON)(?=.*LINE)
				patternShape = Pattern.compile("(?=.)*MULTIPOLYGON");
				//System.out.println(patternShape);
				if (patternShape.matcher(obj).find()) {
					//Pattern patternParanthesis = Pattern.compile("(?<=\\()\\(\\((.*?)\\,");
					//Pattern patternParanthesis = Pattern.compile("(?<=\\()(.*?)\\,");
					Pattern patternParanthesis = Pattern.compile("(?<=\\().*");
					Matcher matcher = patternParanthesis.matcher(obj);
					//System.out.println(matcher.group(1));

					if(matcher.find()) {

						patternParanthesis = Pattern.compile("\\((?=\\()(.*?)(\\))\\)");
						matcher = patternParanthesis.matcher(matcher.group(0));

						while (matcher.find()) {
							System.out.println(matcher.group(0));
							//System.out.println(matcher.group(1));
							findPolygon(obj);
						}
					}
					/*
					Pattern patternParanthesis = Pattern.compile("(?<=\\()\\(\\((.*?)(\\))\\)");
					Matcher matcher = patternParanthesis.matcher(obj);
					//System.out.println(matcher.group(1));

					while(matcher.find())
					{
						System.out.println(matcher.group(0));
						System.out.println(matcher.group(1));
					}

					Pattern pattern = Pattern.compile("(\\()\\((.*?)(\\))\\)");
					matcher = pattern.matcher(obj);

					//List<String> listMatches = new ArrayList<String>();

					while(matcher.find())
					{
						System.out.println(matcher.group(0));
						System.out.println(matcher.group(1));
					}
				/*	Pattern patternParanthesis = Pattern.compile("(?<=\\()(.*?)\\,");
					Matcher matcher = patternParanthesis.matcher(obj);
					//while()
					if (matcher.find()) {
						System.out.println(matcher.group(1));
						String polyStart= matcher.group(1);
						//check2 = true;
						Pattern patternURI = Pattern.compile(".*(?<=\\,)(?)\\)\\)");
						System.out.println(matcher.group(1));
						matcher = patternURI.matcher(obj);
						if(matcher.find()){
							String polyEnd= matcher.group(1).trim();
							if (polyStart.equals(polyEnd))
								check3 = true;
						}

					}*/
				}else {
					patternShape = Pattern.compile("(?=.)*POLYGON");
					if (patternShape.matcher(obj).find()) {
						Pattern patternParanthesis = Pattern.compile("(?<=\\()\\((.*?)\\,");
						Matcher matcher = patternParanthesis.matcher(obj);
						if (matcher.find()) {
							System.out.println(matcher.group(1));
							String polyStart= matcher.group(1);
							//check2 = true;
		//					if(matcher.find()){
								Pattern patternURI = Pattern.compile(".*(?<=\\,)(.*?)\\)\\)");
								System.out.println(matcher.group(1));
								matcher = patternURI.matcher(obj);
								if(matcher.find()){
									String polyEnd= matcher.group(1).trim();
									if (polyStart.equals(polyEnd))
										check3 = true;
								}
						//}
						}
					}

				}

				if (!check3){
					this.misuseObjectProperties++;
					this.createProblemModel(subject, predicate, DQMPROB.MisusedDatatypeProperty);
				}
			}
		}
	}

	private void findPolygon(String obj){
		Pattern patternParanthesis = Pattern.compile("\\(\\((.*?)\\,");
		Matcher matcher = patternParanthesis.matcher(obj);
		if (matcher.find()) {
			System.out.println(matcher.group(1));
			System.out.println(matcher.group(0));
			String polyStart= matcher.group(1);
			//check2 = true;
			//					if(matcher.find()){
			Pattern patternURI = Pattern.compile(".*(?<=\\,)(.*?)\\)\\)");
			System.out.println(matcher.group(1));
			matcher = patternURI.matcher(obj);
			if(matcher.find()){
				String polyEnd= matcher.group(1).trim();
				if (polyStart.equals(polyEnd))
					check3 = true;
			}
			//}
		}

	}
	private void createProblemModel(Node resource, Node property, Resource type){
		ProblemReport pr = new ProblemReport(resource, property, type);
		Boolean isAdded = this.problemSampler.add(pr);
		if (!isAdded) pr = null;
	}


	/**
	 * This method computes metric value for the object of this class
	 *
	 * @return (total misuse properties) / (total properties)
	 */

	public double metricValue() {

		double metricValue = 1.0;

		double misused = this.misuseDatatypeProperties + this.misuseObjectProperties;
		if (misused > 0.0)
			metricValue = 1.0 - (misused / this.validPredicates);

		statsLogger.info("Dataset: {}; Number of Misused Datatype Properties: {}, Number of Misused Object Property : {}, Metric Value: {}",this.getDatasetURI(),this.misuseDatatypeProperties, this.misuseObjectProperties, metricValue);
		return metricValue;
	}

	/**
	 * Returns Metric URI
	 *
	 * @return metric URI
	 */
	public Resource getMetricURI() {
		return this.METRIC_URI;
	}

	/**
	 * Returns list of problematic Quads
	 *
	 * @return list of problematic quads
	 */

//	public ProblemList<?> getQualityProblems() {
//		ProblemList<SerialisableModel> tmpProblemList = null;
//		try {
//			if(this.problemList != null && this.problemList.size() > 0) {
//				tmpProblemList = new ProblemList<SerialisableModel>(new ArrayList<SerialisableModel>(this.problemList));
//			} else {
//				tmpProblemList = new ProblemList<SerialisableModel>();
//			}		} catch (ProblemListInitialisationException problemListInitialisationException) {
//			logger.error(problemListInitialisationException.getMessage());
//		}
//		return tmpProblemList;
//	}


	public ProblemList<?> getQualityProblems() {
		ProblemList<Model> tmpProblemList = new ProblemList<Model>();
		if(this.problemSampler != null && this.problemSampler.size() > 0) {
			for(ProblemReport pr : this.problemSampler.getItems()){
				tmpProblemList.getProblemList().add(pr.createProblemModel());
			}
		} else {
			tmpProblemList = new ProblemList<Model>();
		}
		return tmpProblemList;
	}

	@Override
	public boolean isEstimate() {
		return false;
	}

	@Override
	public Resource getAgentURI() {
		return DQM.LuzzuProvenanceAgent;
	}

	private class ProblemReport{

		private Resource type;
		private Node resource;
		private Node property;

		ProblemReport(Node resource, Node property, Resource type){
			this.resource = resource;
			this.property = property;
			this.type = type;
		}

		Model createProblemModel(){
			Model m = ModelFactory.createDefaultModel();

			Resource subject = m.createResource(resource.toString());
			m.add(new StatementImpl(subject, QPRO.exceptionDescription, type));

			if (type.equals(DQMPROB.MisusedDatatypeProperty))
				m.add(new StatementImpl(subject, DQMPROB.hasMisusedDatatypeProperty, m.asRDFNode(property)));
			else
				m.add(new StatementImpl(subject, DQMPROB.hasMisusedObjectProperty, m.asRDFNode(property)));


			return m;
		}
	}
}
