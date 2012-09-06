// Copyright 2011 Splunk, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License.    
// You may obtain a copy of the License at
//                                                                                                        
//   http://www.apache.org/licenses/LICENSE-2.0 
//
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and 
// limitations under the License.

$(document).ready(function() {
    bindHandlers();
});
function bindHandlers() {
    top.$("body").bind('change', resizeIFrames);
}
function resizeIFrames() {
    return top.$("body").trigger("resizeIFrames");
}

switch (Splunk.util.getCurrentView()) {
    case "thaw":
    case "setup":
        if (Splunk.Module.IFrameInclude) {
            Splunk.Module.IFrameInclude = $.klass(Splunk.Module.IFrameInclude, {
                onLoad: function(event) {
                    this.logger.info("IFrameInclude onLoad event fired.");
             
                    this.resize();
                    //this.iframe.contents().find("body").click(this.resize.bind(this));
                    $("body").bind( "resizeBody", this.resize.bind(this) );
                    top.$("body").bind( "resizeIFrames", this.resize.bind(this) );
                },
                
                resize: function() {
                    this.logger.info("IFrameInclude resize fired.");
                    
                    var height = this.getHeight();
                    if(height<1){
                        this.iframe[0].style.height = "auto";
                        this.iframe[0].scrolling = "auto";
                    }else{
                        this.iframe[0].style.height = height + this.IFRAME_HEIGHT_FIX + "px";
                        this.iframe[0].scrolling = "yes";
                    }
                }
            });
        }
        break;
}
