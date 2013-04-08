/*******************************************************************************
 * Copyright (c) 2013 The Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Wayne Beaton - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.foundation.cla;

import com.google.gerrit.sshd.PluginCommandModule;

class SshModule extends PluginCommandModule {

  @Override
  protected void configureCommands() {
    // command("my-command").to(MyCommand.class);
  }
}
