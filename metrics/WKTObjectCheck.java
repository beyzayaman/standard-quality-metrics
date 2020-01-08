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
 * This metric calculates the number of valid Number of RDFS Literals of type geo:wktLiteral properties consisting of an optional URI
 * identifying the coordinate reference system followed by Simple Features Well Known Text (WKT) describing a geometric value. This metric checks
 * if instances in a dataset have geo:asWKT object conforming to the standard.
 *
 *
 * Based on:
 *
 * OGC Requirement: All RDFS Literals of type geo:wktLiteral shall consist of an optional URI
 * identifying the coordinate reference system followed by Simple Features Well Known
 * Text (WKT) describing a geometric value. Valid geo:wktLiterals are formed by
 * concatenating a valid, absolute URI as defined in [RFC 2396], one or more spaces
 * (Unicode U+0020 character) as a separator, and a WKT string as defined in Simple
 * Features [ISO 19125-1].
 * 
 *
 */
public class WKTObjectCheck extends AbstractQualityMetric {

	private final Resource METRIC_URI = DQM.WKTObjectCompletenessMetric;

	private static Logger logger = LoggerFactory.getLogger(WKTObjectCheck.class);

	// Sampling of problems - testing for LOD EWKTPropertyCheckvaluation
	ReservoirSampler<ProblemReport> problemSampler = new ReservoirSampler<ProblemReport>(50000, false);

	private double misusedASWKTProperties = 0.0;
	private int propertyCount=0;

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

		//String greekProp= "http://geo.linkedopendata.gr/coastline_gr/ontology#hasGeometry";
		boolean check = false;

		Pattern patternShape;//(?=.*MULTIPOLYGON)(?=.*MULTIPOLYGON)(?=.*LINE)
		patternShape = Pattern.compile("(?=.)*MULTIPOLYGON|(?=.)*POLYGON|(?=.)*LINE|(?=.)*POINT|(?=.)*MULTILINESTRING");

		if ((predicate.getURI().equals(GEOSPARQL.asWKTproperty.getURI()))){

			propertyCount++;

			if (object.isLiteral()){
				//String str = "ZZZZL <%= dsn %> AFFF <%= AFG %>";
				String obj= object.toString();
				if (patternShape.matcher(obj).find()) {
					Pattern patternParanthesis = Pattern.compile("\\((.*?)\\)\"", Pattern.DOTALL);
					Matcher matcher = patternParanthesis.matcher(obj);
					if (matcher.find()) {
						//System.out.println(matcher.group(1));
						Pattern patternURI = Pattern.compile("(?=.)*http://www.opengis.net/ont/geosparql#wktLiteral");
						matcher = patternURI.matcher(obj);
						if(matcher.find()){
							check = true;}
					}
				}

				if (!check){
					this.misusedASWKTProperties++;
					this.createProblemModel(subject, predicate, DQMPROB.NonStandardASWKTGeometryDefinition);
				}
			}
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

		double misused = this.misusedASWKTProperties;
		if (misused > 0.0)
			metricValue = 1.0 - (this.misusedASWKTProperties / this.propertyCount);

		statsLogger.info("Dataset: {}; Missing AsWKT Objects: {}, Number of total instances: {}, Metric Value: {}",this.getDatasetURI(),this.misusedASWKTProperties, this.propertyCount, metricValue);
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
