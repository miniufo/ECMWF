package org.ecmwf;

import java.io.IOException;

/*
 *
 * (C) Copyright 2012-2013 ECMWF.
 *
 * This software is licensed under the terms of the Apache Licence Version 2.0
 * which can be obtained at http://www.apache.org/licenses/LICENSE-2.0. 
 * In applying this licence, ECMWF does not waive the privileges and immunities 
 * granted to it by virtue of its status as an intergovernmental organisation nor
 * does it submit to any jurisdiction.
 *
 */

import org.json.JSONStringer;

public class Service extends Retriever {

	private String service;

	public Service(String service)
	{
		this.service = service;
	}

	public void retrieve(String req, String target) throws IOException, APIError {
		JSONStringer s = new JSONStringer();
		req = s.array().value(req).endArray().toString();
		req = req.substring(1, req.length()-1);
		retrieve("/services/" + service, req, target);
	}

}
