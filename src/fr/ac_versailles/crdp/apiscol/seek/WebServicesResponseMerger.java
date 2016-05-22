package fr.ac_versailles.crdp.apiscol.seek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class WebServicesResponseMerger {

	private static NamespaceContext ctx = new NamespaceContext() {
		public String getNamespaceURI(String prefix) {
			String uri;
			if (prefix.equals(UsedNamespaces.ATOM.getShortHand()))
				uri = UsedNamespaces.ATOM.getUri();
			else if (prefix.equals(UsedNamespaces.APISCOL.getShortHand())) {
				uri = UsedNamespaces.APISCOL.getUri();
			} else
				uri = null;
			return uri;
		}

		public Iterator getPrefixes(String val) {
			return null;
		}

		public String getPrefix(String uri) {
			return null;
		}
	};
	private static XPathFactory xPathFactory = XPathFactory.newInstance();
	private static XPath xpath = xPathFactory.newXPath();

	public static void mergeHits(Document metadataResponse,
			HashMap<String, NodeList> metadataFromExtractedResponse) {
		createLogger();
		assignNamespaceContext();

		Iterator<String> it = metadataFromExtractedResponse.keySet().iterator();
		while (it.hasNext()) {

			String metadataUrl = (String) it.next().trim();
			try {
				XPathExpression exp = xpath
						.compile("//atom:entry[atom:link[@href='" + metadataUrl
								+ "']]/atom:id");
				Node idNode = (Node) exp.evaluate(metadataResponse,
						XPathConstants.NODE);
				if (idNode == null) {
					logger.warn("No correspondency found in metadata webservice response for metadata id found in content web service response : ["
							+ metadataUrl + "]");
					continue;
				}
				String metadataUrn = idNode.getTextContent();
				XPathExpression exp2 = xpath
						.compile("//apiscol:hits/apiscol:hit[@metadataId='"
								+ metadataUrn + "']/apiscol:matches");
				Node matchesNode = (Node) exp2.evaluate(metadataResponse,
						XPathConstants.NODE);

				NodeList newMatches = metadataFromExtractedResponse
						.get(metadataUrl);

				for (int i = 0; i < newMatches.getLength(); i++) {
					Element newMatch = (Element) metadataResponse.importNode(
							newMatches.item(i), true);
					newMatch.setAttribute("source", "data");
					matchesNode.appendChild(newMatch);
				}
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static Document mergeSuggestions(Document contentResponse,
			Document metadataResponse) {
		// TODO une méthode d'initialisation
		createLogger();
		assignNamespaceContext();
		XPathExpression exp = null;
		try {
			exp = xpath.compile("//apiscol:query_term");
			NodeList queryTermsNodes = (NodeList) exp.evaluate(contentResponse,
					XPathConstants.NODESET);
			for (int i = 0; i < queryTermsNodes.getLength(); i++) {
				Element queryTerm = (Element) queryTermsNodes.item(i);
				String requestedTerm = queryTerm.getAttribute("requested");
				XPathExpression exp2 = xpath
						.compile("//apiscol:query_term[@requested='"
								+ requestedTerm + "']");
				Node queryNode = (Node) exp2.evaluate(metadataResponse,
						XPathConstants.NODE);
				if (queryNode != null) {
					XPathExpression exp3 = xpath
							.compile("//apiscol:query_term[@requested='"
									+ requestedTerm + "']/apiscol:word");
					NodeList wordNodes = (NodeList) exp3.evaluate(
							contentResponse, XPathConstants.NODESET);
					for (int j = 0; j < wordNodes.getLength(); j++) {
						Element newMatch = (Element) metadataResponse
								.importNode(wordNodes.item(j), true);
						newMatch.setAttribute("source", "data");
						queryNode.appendChild(newMatch);
					}
				} else {
					XPathExpression exp4 = xpath
							.compile("//apiscol:spellcheck");
					Node spellCheckNode = (Node) exp4.evaluate(
							metadataResponse, XPathConstants.NODE);
					Element importedQueryTerm = (Element) metadataResponse
							.importNode(queryTerm, true);
					importedQueryTerm.setAttribute("source", "data");
					if (spellCheckNode != null)
						spellCheckNode.appendChild(importedQueryTerm);
				}

			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return metadataResponse;
	}

	private static void assignNamespaceContext() {
		xpath.setNamespaceContext(ctx);
	}

	public static Document mergeSpellChecks(Document contentResponse,
			Document metadataResponse) {
		createLogger();
		assignNamespaceContext();
		XPathExpression exp = null;
		try {
			// select all query terms tag from content response
			exp = xpath.compile("//apiscol:query_term");
			NodeList queryTermsNodes = (NodeList) exp.evaluate(contentResponse,
					XPathConstants.NODESET);
			for (int i = 0; i < queryTermsNodes.getLength(); i++) {
				Element queryTerm = (Element) queryTermsNodes.item(i);
				// for each query term tag, extract requested attribute
				String requestedTerm = queryTerm.getAttribute("requested");

				// loook for query term with the same attribute in metadata
				// response
				XPathExpression exp2 = xpath
						.compile("//apiscol:query_term[@requested='"
								+ requestedTerm + "']");
				Node queryNode = (Node) exp2.evaluate(metadataResponse,
						XPathConstants.NODE);
				if (queryNode != null) {
					// you did find one, append, the words from content response
					XPathExpression exp3 = xpath
							.compile("//apiscol:query_term[@requested='"
									+ requestedTerm + "']/apiscol:word");
					NodeList wordNodes = (NodeList) exp3.evaluate(
							contentResponse, XPathConstants.NODESET);
					for (int j = 0; j < wordNodes.getLength(); j++) {
						Element newMatch = (Element) metadataResponse
								.importNode(wordNodes.item(j), true);
						newMatch.setAttribute("source", "data");
						queryNode.appendChild(newMatch);
					}
				} else {
					// you didn't, append the whole query term tag
					XPathExpression exp4 = xpath
							.compile("//apiscol:spellcheck");
					Node spellCheckNode = (Node) exp4.evaluate(
							metadataResponse, XPathConstants.NODE);
					Element importedQueryTerm = (Element) metadataResponse
							.importNode(queryTerm, true);
					importedQueryTerm.setAttribute("source", "data");
					if (spellCheckNode != null)
						spellCheckNode.appendChild(importedQueryTerm);
				}

			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return metadataResponse;

	}

	public static List<String> extractMetadataList(Document mergedResponse) {
		createLogger();
		assignNamespaceContext();
		XPathExpression exp;
		NodeList resultat = null;
		try {
			exp = xpath
					.compile("/atom:feed/apiscol:metadata/atom:link[@rel='self'][@type='text/html']");
			resultat = (NodeList) exp.evaluate(mergedResponse,
					XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<String> list = new ArrayList<String>();
		if (resultat == null) {
			logger.warn("MetadataList is null after merging metadata web service response and content web service response");
			return list;
		}
		int nbResults = resultat.getLength();
		for (int i = 0; i < nbResults; i++) {
			String metadataLink = ((Element) resultat.item(i))
					.getAttribute("href");
			list.add(metadataLink);

		}
		return list;
	}

	public static HashMap<String, NodeList> collectMetadataAndHits(
			Document contentResponse) {
		createLogger();
		assignNamespaceContext();
		XPathExpression exp;
		NodeList resultat = null;
		// catch metadatas
		try {
			exp = xpath.compile("//atom:link[@rel='describedby']");
			resultat = (NodeList) exp.evaluate(contentResponse,
					XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int nbResults = resultat.getLength();
		HashMap<String, NodeList> list = new HashMap<String, NodeList>();
		// look for the urn
		for (int i = 0; i < nbResults; i++) {
			String metadataUrl = ((Element) resultat.item(i))
					.getAttribute("href");
			XPathExpression exp2 = null;
			try {
				exp2 = xpath.compile("//atom:entry[atom:link[@href='"
						+ metadataUrl + "']]/atom:id");
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String resourceUrn = "";
			try {
				resourceUrn = ((Node) exp2.evaluate(contentResponse,
						XPathConstants.NODE)).getTextContent();
			} catch (DOMException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (XPathExpressionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			XPathExpression exp3 = null;
			try {
				exp3 = xpath
						.compile("/atom:feed/apiscol:hits/apiscol:hit[@resourceId='"
								+ resourceUrn
								+ "']/apiscol:matches/apiscol:match");
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			NodeList hitsNodes = null;
			try {
				hitsNodes = (NodeList) exp3.evaluate(contentResponse,
						XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			list.put(metadataUrl, hitsNodes);
		}
		return list;
	}

	private static Logger logger;

	private static void createLogger() {
		if (logger == null)
			logger = LogUtility.createLogger(WebServicesResponseMerger.class
					.getCanonicalName());
	}

	public static void addThumbsReferences(Document metadataResponse,
			Document thumbsResponse) {
		createLogger();
		assignNamespaceContext();
		XPathExpression exp = null;
		try {
			// select all query terms tag from content response
			exp = xpath.compile("//apiscol:thumb");
			NodeList thumbsNodes = (NodeList) exp.evaluate(thumbsResponse,
					XPathConstants.NODESET);
			for (int i = 0; i < thumbsNodes.getLength(); i++) {
				Element thumbNode = (Element) thumbsNodes.item(i);
				// for each query term tag, extract requested attribute
				String metadataId = thumbNode.getAttribute("mdid");
				// look for query term with the same attribute in metadata
				// response
				XPathExpression exp2 = xpath
						.compile("//apiscol:metadata[atom:link[@href='"
								+ metadataId + "']]");
				Node metadataNode = (Node) exp2.evaluate(metadataResponse,
						XPathConstants.NODE);
				if (metadataNode != null) {

					Element importedThumbNode = (Element) metadataResponse
							.importNode(thumbNode, true);
					if (importedThumbNode != null)
						metadataNode.appendChild(importedThumbNode);
				}

			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void addContentReferences(Document metadataResponse,
			Document contentResponse) {
		createLogger();
		assignNamespaceContext();
		XPathExpression exp = null;
		Element contentNode = metadataResponse.createElement("content");
		metadataResponse.getDocumentElement().appendChild(contentNode);
		try {
			// select all query terms tag from content response
			exp = xpath.compile("/apiscol:resource/apiscol:type");
			Node typeNode = (Node) exp.evaluate(contentResponse,
					XPathConstants.NODE);
			String type = typeNode.getTextContent();
			// look for query term with the same attribute in metadata
			// response

			Element importedContentNode = (Element) metadataResponse
					.importNode(typeNode, true);
			if (importedContentNode != null)
				contentNode.appendChild(importedContentNode);
			Node urlNode;
			if (type.equals("url")) {
				exp = xpath.compile("//apiscol:url");
				urlNode = (Node) exp.evaluate(contentResponse,
						XPathConstants.NODE);

			} else {
				exp = xpath.compile("//atom:link[@rel='download']");
				urlNode = (Node) exp.evaluate(contentResponse,
						XPathConstants.NODE);

			}
			if (urlNode != null) {
				Element importedUrlNode = (Element) metadataResponse
						.importNode(urlNode, true);
				if (importedUrlNode != null)
					contentNode.appendChild(importedUrlNode);
			}
			XPathExpression exp2 = xpath
					.compile("//apiscol:link[@rel='preview']");
			Node previewNode = (Node) exp2.evaluate(contentResponse,
					XPathConstants.NODE);
			if (previewNode != null) {
				Element importedPreviewNode = (Element) metadataResponse
						.importNode(previewNode, true);
				if (importedPreviewNode != null)
					contentNode.appendChild(importedPreviewNode);
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
