/*
 * Copyright (c) 2009 Creative Sphere Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   Creative Sphere - initial API and implementation
 *
 */
package org.abstracthorizon.mercury.adminconsole;

import org.abstracthorizon.danube.connection.Connection;
import org.abstracthorizon.danube.mvc.ModelAndView;
import org.abstracthorizon.danube.mvc.View;

/**
 *
 * @author Daniel Sendula
 */
public class ViewAdapterInterceptor implements View {

    private View def;

    private View json;
    
    public void render(Connection connection, ModelAndView modelAndView) {
        String view = modelAndView.getView(); 
        if (view.startsWith("json/bean")) {
            json.render(connection, modelAndView);
        } else {
            def.render(connection, modelAndView);
        }
    }

    public View getDefault() {
        return def;
    }


    public void setDefault(View def) {
        this.def = def;
    }


    public View getJson() {
        return json;
    }

    public void setJson(View json) {
        this.json = json;
    }

}
