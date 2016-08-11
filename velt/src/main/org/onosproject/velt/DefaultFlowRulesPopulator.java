/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.velt;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class DefaultFlowRulesPopulator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    public static final String SR_APP_ID = "org.onosproject.velt";
    public static final String DEVICEID = "of:0000000000000001";
    public static final int DEFAULTPRIORITY = 60000;
    public static final long DATAIF = 26;
    public static final long VMIF1 = 30;
    public static final long VMIF2 = 31;
    public static final short SITEVLAN = 100;
    public static final short TEMVLAN = 200;

    protected DeviceId bridgeID;
    protected ApplicationId appId;


    @Activate
    protected void activate() {

        log.info("Started");
        bridgeID = DeviceId.deviceId(DEVICEID);

        for (Device device : deviceService.getDevices()) {
            if (device.id().equals(bridgeID)) {

                log.info("Populate flow rule for:000000000000001");

                installDefaultRule(PortNumber.portNumber(DATAIF), PortNumber.portNumber(VMIF1),
                                   SITEVLAN, (short) 0);
                installDefaultRule(PortNumber.portNumber(VMIF2), PortNumber.portNumber(DATAIF),
                                   SITEVLAN, TEMVLAN);
                installDefaultRule(PortNumber.portNumber(DATAIF), PortNumber.portNumber(VMIF2),
                                   TEMVLAN, (short) 0);
                installDefaultRule(PortNumber.portNumber(VMIF1), PortNumber.portNumber(DATAIF),
                                   TEMVLAN, SITEVLAN);
            }
        }

    }

    @Deactivate
    protected void deactivate() {

        flowRuleService.removeFlowRulesById(appId);

        log.info("Stopped");
    }

    protected void installDefaultRule(PortNumber inPortNumber, PortNumber outPortNumber, short inVlan, short outVlan) {
        appId = coreService.registerApplication(SR_APP_ID);

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                .matchInPort(inPortNumber)
                .matchVlanId(VlanId.vlanId(inVlan));


        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();

        if (outVlan != (short) 0) {
            builder.setVlanId(VlanId.vlanId(outVlan));
        }

        TrafficTreatment treatment = builder.setOutput(outPortNumber)
                .build();

        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withPriority(DEFAULTPRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent()
                .add();

        flowObjectiveService.forward(DeviceId.deviceId(DEVICEID),
                                     forwardingObjective);
    }

}
