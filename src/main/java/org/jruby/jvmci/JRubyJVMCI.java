package org.jruby.jvmci;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompilationRequestResult;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.hotspot.HotSpotCompiledCode;
import jdk.vm.ci.hotspot.HotSpotInstalledCode;
import jdk.vm.ci.hotspot.HotSpotJVMCICompilerFactory;
import jdk.vm.ci.hotspot.HotSpotVMEventListener;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.JVMCICompiler;
import jdk.vm.ci.runtime.JVMCICompilerFactory;
import jdk.vm.ci.runtime.JVMCIRuntime;
import jdk.vm.ci.services.JVMCIServiceLocator;
import org.graalvm.compiler.core.phases.CoreCompilerConfiguration;
import org.graalvm.compiler.core.phases.HighTier;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.hotspot.CompilerConfigurationFactory;
import org.graalvm.compiler.hotspot.CoreCompilerConfigurationFactory;
import org.graalvm.compiler.hotspot.HotSpotGraalCompiler;
import org.graalvm.compiler.hotspot.HotSpotGraalCompilerFactory;
import org.graalvm.compiler.hotspot.HotSpotGraalJVMCIServiceLocator;
import org.graalvm.compiler.hotspot.HotSpotGraalOptionValues;
import org.graalvm.compiler.nodes.FrameState;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.java.MonitorIdNode;
import org.graalvm.compiler.nodes.java.NewInstanceNode;
import org.graalvm.compiler.nodes.spi.VirtualizerTool;
import org.graalvm.compiler.nodes.virtual.VirtualInstanceNode;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.PhaseSuite;
import org.graalvm.compiler.phases.tiers.CompilerConfiguration;
import org.graalvm.compiler.phases.tiers.HighTierContext;
import org.graalvm.compiler.phases.tiers.PhaseContext;
import org.graalvm.compiler.virtual.phases.ea.PartialEscapePhase;

import java.util.Collections;
import java.util.ListIterator;

public class JRubyJVMCI {
    public static class ServiceLocator extends JVMCIServiceLocator {
        HotSpotGraalJVMCIServiceLocator locator = new HotSpotGraalJVMCIServiceLocator();

        @Override
        public <S> S getProvider(Class<S> service) {
            if (service == JVMCICompilerFactory.class) {
                return (S) new CompilerFactory((HotSpotGraalCompilerFactory) locator.getProvider(service));
            }

            return locator.getProvider(service);
        }
    }

    public static class CompilerFactory extends HotSpotJVMCICompilerFactory {
        HotSpotGraalCompilerFactory factory;

        public CompilerFactory(HotSpotGraalCompilerFactory factory) {
            this.factory = factory;
        }

        @Override
        public String getCompilerName() {
            return "jruby-graal";
        }

        @Override
        public JVMCICompiler createCompiler(JVMCIRuntime jvmciRuntime) {
            OptionValues options = HotSpotGraalOptionValues.HOTSPOT_OPTIONS;
//            final CompilerConfigurationFactory configFactory = CompilerConfigurationFactory.selectFactory((String)null, options);
            CompilerConfigurationFactory jrubyConfigFactory = new JRubyCompilerConfigurationFactory();
            Compiler compiler = new Compiler(factory.createCompiler(jvmciRuntime, options, jrubyConfigFactory));
            return compiler;
        }

        @Override
        public void onSelection() {
            factory.onSelection();
        }

        @Override
        public CompilationLevelAdjustment getCompilationLevelAdjustment() {
            return factory.getCompilationLevelAdjustment();
        }

        @Override
        public CompilationLevel adjustCompilationLevel(Class<?> declaringClass, String name, String signature, boolean isOsr, CompilationLevel level) {
            return factory.adjustCompilationLevel(declaringClass, name, signature, isOsr, level);
        }
    }

    public static class JRubyCompilerConfigurationFactory extends CoreCompilerConfigurationFactory {
        @Override
        public CompilerConfiguration createCompilerConfiguration() {
//            return new CoreCompilerConfiguration();
            return new JRubyGraalCompilerConfiguration();
        }
    }

    public static class JRubyGraalCompilerConfiguration extends CoreCompilerConfiguration {
        @Override
        public PhaseSuite<HighTierContext> createHighTier(OptionValues options) {
            HighTier highTier = new HighTier(options);
            ListIterator iter = highTier.findPhase(PartialEscapePhase.class);
            iter.previous();
            iter.add(new JRubyVirtualizationPhase());
            return highTier;
        }
    }

    public static class Compiler implements JVMCICompiler {
        HotSpotGraalCompiler compiler;

        public Compiler(HotSpotGraalCompiler compiler) {
            this.compiler = compiler;
        }

        @Override
        public CompilationRequestResult compileMethod(CompilationRequest compilationRequest) {
            CompilationRequestResult result = compiler.compileMethod(compilationRequest);

            return result;
        }
    }

    public static class HotspotListener implements HotSpotVMEventListener {
        public void notifyShutdown() {
        }

        public void notifyInstall(HotSpotCodeCacheProvider hotSpotCodeCacheProvider, HotSpotInstalledCode installedCode, HotSpotCompiledCode compiledCode) {
            System.out.println("compiled code installed: " + compiledCode.getName());
        }

        public void notifyBootstrapFinished() {
        }
    }

    public static class JRubyVirtualizationPhase extends BasePhase<PhaseContext> {

        public JRubyVirtualizationPhase() {
            super();
        }

        @Override
        protected void run(StructuredGraph structuredGraph, PhaseContext phaseContext) {
            NodeIterable<Node> nodes = structuredGraph.getNodes();
            nodes.forEach(n -> {
                if (n.getClass() == NewInstanceNode.class) {
                    NewInstanceNode newInstance = (NewInstanceNode) n;
                    if (isVirtual(newInstance.instanceClass())) {
                        System.out.println("virtualizing fixnum: " + newInstance);
                        JRubyNewInstanceNode jnin = structuredGraph.add(new JRubyNewInstanceNode(newInstance.instanceClass(), newInstance.fillContents(), newInstance.stateBefore()));
                        structuredGraph.replaceFixedWithFixed(newInstance, jnin);
                    }
                }
            });
        }

        private boolean isVirtual(ResolvedJavaType type) {
            String name = type.getName();

            if (name.contains("RubyFixnum") || name.contains("RubyFloat")) return true;

            return false;
        }
    }

    public static class JRubyNewInstanceNode extends NewInstanceNode {

        public JRubyNewInstanceNode(ResolvedJavaType type, boolean fillContents, FrameState stateBefore) {
            super(type, fillContents, stateBefore);
        }

        @Override
        public void virtualize(VirtualizerTool tool) {
            /*
             * This is always for virtualizable JRuby objects, so always virtualize.
             */
            VirtualInstanceNode virtualObject = new VirtualInstanceNode(instanceClass(), false);
            ResolvedJavaField[] fields = virtualObject.getFields();
            ValueNode[] state = new ValueNode[fields.length];
            for (int i = 0; i < state.length; i++) {
                state[i] = defaultFieldValue(fields[i]);
            }
            tool.createVirtualObject(virtualObject, state, Collections.<MonitorIdNode> emptyList(), false);
            tool.replaceWithVirtual(virtualObject);
        }
    }


}
