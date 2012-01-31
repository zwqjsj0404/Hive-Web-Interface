/*
 * Copyright (c) 2012, someone All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1.Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2.Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. 3.Neither the name of the Happyelements Ltd. nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.happyelements.hive.web.api;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.mapred.JobStatus;

import com.happyelements.hive.web.Authorizer;
import com.happyelements.hive.web.HadoopClient;
import com.happyelements.hive.web.HTTPServer.HTTPHandler;
import com.happyelements.hive.web.HadoopClient.QueryInfo;

/**
 * @author <a href="mailto:zhizhong.qiu@happyelements.com">kevin</a>
 *
 */
public class GetUserQuerys extends HTTPHandler {

	/**
	 * {@inheritDoc}}
	 */
	public GetUserQuerys(String url) {
		super(true, url);
	}

	/**
	 * @see com.happyelements.hive.web.HTTPServer.HTTPHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void handle(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		// check method
		if (!"GET".equals(request.getMethod())) {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"do not supoort http method except for GET");
			return;
		}

		// check user
		String user = Authorizer.extractUser(request);
		if (user == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"find no user name");
			return;
		}

		// get now
		long now = System.currentTimeMillis();

		// set up standard responses header
		response.setCharacterEncoding("utf8");
		response.setContentType("application/json");

		// get job status
		StringBuilder builder = new StringBuilder("{\"querys\":[");
		for (JobStatus job : HadoopClient.getAllJobs()) {
			// find query info
			QueryInfo info = HadoopClient.getQueryInfo(job.getJobID()
					.getJtIdentifier());

			// update access time
			info.access = now;

			// if match user
			if (user.equals(info.user)) {
				builder.append("{\"id\":\"" + info.query_id + "\",");
				switch (job.getRunState()) {
				case JobStatus.RUNNING:
					builder.append("\"status\":\"RUNNING\",\"map\":"
							+ job.mapProgress() + ",\"reduce\":"
							+ job.reduceProgress());
					break;
				case JobStatus.FAILED:
					builder.append("\"status\":\"FAILED\",\"map\":"
							+ job.mapProgress() + ",\"reduce\":"
							+ job.reduceProgress());
					break;
				case JobStatus.KILLED:
					builder.append("\"status\":\"KILLED\",\"map\":"
							+ job.mapProgress() + ",\"reduce\":"
							+ job.reduceProgress());
					break;
				case JobStatus.PREP:
					builder.append("\"status\":\"PREP\",\"map\":"
							+ job.mapProgress() + ",\"reduce\":"
							+ job.reduceProgress());
					break;
				case JobStatus.SUCCEEDED:
					builder.append("\"status\":\"SUCCEEDED\",\"map\":"
							+ job.mapProgress() + ",\"reduce\":"
							+ job.reduceProgress());
					break;
				}
				builder.append(",\"query\":\"" + info.query + "\"},");
			}
		}

		if (builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}

		response.getWriter().print(builder.append("]}").toString());
		return;
	}
}