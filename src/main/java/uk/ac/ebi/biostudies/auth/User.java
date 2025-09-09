/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.auth;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class User {
    protected String login;
    protected String fullName;
    protected String token;
    protected String[] allow;
    protected String[] deny;
    protected String email;
    protected boolean superUser;

    public Set<GrantedAuthority> getAuthorities(){
        Set<GrantedAuthority> grantedAuths = new HashSet();
        if(allow!=null && allow.length>0)
            for(int i=0; i<allow.length; i++)
                grantedAuths.add(new SimpleGrantedAuthority(allow[i]));
        if(superUser)
            grantedAuths.add(new SimpleGrantedAuthority("SUPER_USER"));
        return grantedAuths;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String[] getAllow() {
        return allow;
    }

    public void setAllow(String[] allow) {
        this.allow = allow;
    }

    public String[] getDeny() {
        return deny;
    }

    public void setDeny(String[] deny) {
        this.deny = deny;
    }

    public boolean isSuperUser() { return superUser; }

    public void setSuperUser(boolean superUser) { this.superUser = superUser; }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return StringUtils.isNotBlank(login) ? login :
                StringUtils.isNotBlank(fullName) ? fullName :
                        email;
    }

}
