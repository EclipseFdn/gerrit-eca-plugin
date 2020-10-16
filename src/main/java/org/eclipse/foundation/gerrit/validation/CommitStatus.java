/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.foundation.gerrit.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information generated about a commit that was submitted for
 * validation to the API.
 * 
 * @author Martin Lowe
 *
 */
public class CommitStatus {
	private List<CommitStatusMessage> messages;
	private List<CommitStatusMessage> warnings;
	private List<CommitStatusMessage> errors;

	public CommitStatus() {
		this.messages = new ArrayList<>();
		this.warnings = new ArrayList<>();
		this.errors = new ArrayList<>();
	}

	/**
	 * @return the msgs
	 */
	public List<CommitStatusMessage> getMessages() {
		return new ArrayList<>(this.messages);
	}

	/**
	 * @param messages the msgs to set
	 */
	public void setMessages(List<CommitStatusMessage> messages) {
		this.messages = new ArrayList<>(messages);
	}

	/**
	 * @param message message to add to current commit status
	 * @param code    the status code for the message
	 */
	public void addMessage(String message, APIStatusCode code) {
		this.messages.add(new CommitStatusMessage(code, message));
	}

	/**
	 * @return the warnings
	 */
	public List<CommitStatusMessage> getWarnings() {
		return new ArrayList<>(this.warnings);
	}

	/**
	 * @param warnings the warnings to set
	 */
	public void setWarnings(List<CommitStatusMessage> warnings) {
		this.warnings = new ArrayList<>(warnings);
	}

	/**
	 * @param warning warning to add to current commit status
	 * @param code    the status code for the message
	 */
	public void addWarning(String warning, APIStatusCode code) {
		this.warnings.add(new CommitStatusMessage(code, warning));
	}

	/**
	 * @return the errs
	 */
	public List<CommitStatusMessage> getErrors() {
		return new ArrayList<>(this.errors);
	}

	/**
	 * @param errors the errors to set
	 */
	public void setErrors(List<CommitStatusMessage> errors) {
		this.errors = new ArrayList<>(errors);
	}

	/**
	 * @param error error message to add to current commit status
	 * @param code  the error status for the current message
	 */
	public void addError(String error, APIStatusCode code) {
		this.errors.add(new CommitStatusMessage(code, error));
	}

	/**
	 * Represents a message with an associated error or success status code.
	 * 
	 * @author Martin Lowe
	 *
	 */
	public static class CommitStatusMessage {
		private APIStatusCode code;
		private String message;

		public CommitStatusMessage(APIStatusCode code, String message) {
			this.code = code;
			this.message = message;
		}

		/**
		 * @return the code
		 */
		public APIStatusCode getCode() {
			return this.code;
		}

		/**
		 * @param code the code to set
		 */
		public void setCode(APIStatusCode code) {
			this.code = code;
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return this.message;
		}

		/**
		 * @param message the message to set
		 */
		public void setMessage(String message) {
			this.message = message;
		}
	}
}
