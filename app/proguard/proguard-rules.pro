# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontobfuscate

# TODO: Shrinking is broken
-dontshrink

# AGPBI META-INF/services warnings
-dontwarn org.apache.xalan.extensions.bsf.BSFManager
-dontwarn org.w3c.dom.DOMImplementationSourceList
-dontwarn org.xml.sax.driver

# For AndroidX Paging, seems to be a library config error
-keep class org.xml.sax.helpers.NamespaceSupport$Context

# For AndroidX Compose, seems to be a library config error
-keep class org.xml.sax.helpers.AttributesImpl

-dontwarn com.github.luben.zstd.BufferPool
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn com.github.luben.zstd.ZstdOutputStream
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.oracle.svm.core.annotate.AutomaticFeature
-dontwarn com.oracle.svm.core.annotate.Delete
-dontwarn com.oracle.svm.core.annotate.Substitute
-dontwarn com.oracle.svm.core.annotate.TargetClass
-dontwarn com.oracle.svm.core.configure.ResourcesRegistry
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn groovy.lang.Binding
-dontwarn groovy.lang.Closure
-dontwarn groovy.lang.GroovyClassLoader
-dontwarn groovy.lang.GroovyObject
-dontwarn groovy.lang.GroovyShell
-dontwarn groovy.lang.MetaClass
-dontwarn groovy.lang.MetaProperty
-dontwarn groovy.lang.Reference
-dontwarn groovy.lang.Script
-dontwarn java.applet.Applet
-dontwarn java.applet.AppletContext
-dontwarn java.applet.AppletStub
-dontwarn java.applet.AudioClip
-dontwarn java.awt.AWTEvent
-dontwarn java.awt.ActiveEvent
-dontwarn java.awt.AlphaComposite
-dontwarn java.awt.BasicStroke
-dontwarn java.awt.BorderLayout
-dontwarn java.awt.CardLayout
-dontwarn java.awt.Color
-dontwarn java.awt.Component
-dontwarn java.awt.Composite
-dontwarn java.awt.Container
-dontwarn java.awt.Dialog
-dontwarn java.awt.Dimension
-dontwarn java.awt.EventQueue
-dontwarn java.awt.FlowLayout
-dontwarn java.awt.Font
-dontwarn java.awt.FontMetrics
-dontwarn java.awt.Frame
-dontwarn java.awt.Graphics
-dontwarn java.awt.Graphics2D
-dontwarn java.awt.GridBagConstraints
-dontwarn java.awt.GridBagLayout
-dontwarn java.awt.GridLayout
-dontwarn java.awt.Image
-dontwarn java.awt.Insets
-dontwarn java.awt.Label
-dontwarn java.awt.LayoutManager
-dontwarn java.awt.MenuComponent
-dontwarn java.awt.Point
-dontwarn java.awt.Polygon
-dontwarn java.awt.Rectangle
-dontwarn java.awt.RenderingHints
-dontwarn java.awt.RenderingHints$Key
-dontwarn java.awt.Shape
-dontwarn java.awt.Stroke
-dontwarn java.awt.SystemColor
-dontwarn java.awt.TextArea
-dontwarn java.awt.TextComponent
-dontwarn java.awt.TextField
-dontwarn java.awt.Toolkit
-dontwarn java.awt.Window
-dontwarn java.awt.datatransfer.Clipboard
-dontwarn java.awt.datatransfer.ClipboardOwner
-dontwarn java.awt.datatransfer.DataFlavor
-dontwarn java.awt.datatransfer.StringSelection
-dontwarn java.awt.datatransfer.Transferable
-dontwarn java.awt.datatransfer.UnsupportedFlavorException
-dontwarn java.awt.event.ActionEvent
-dontwarn java.awt.event.ActionListener
-dontwarn java.awt.event.ComponentEvent
-dontwarn java.awt.event.ComponentListener
-dontwarn java.awt.event.ContainerEvent
-dontwarn java.awt.event.ContainerListener
-dontwarn java.awt.event.KeyAdapter
-dontwarn java.awt.event.KeyEvent
-dontwarn java.awt.event.KeyListener
-dontwarn java.awt.event.MouseAdapter
-dontwarn java.awt.event.MouseEvent
-dontwarn java.awt.event.MouseListener
-dontwarn java.awt.event.TextEvent
-dontwarn java.awt.event.TextListener
-dontwarn java.awt.event.WindowAdapter
-dontwarn java.awt.event.WindowEvent
-dontwarn java.awt.event.WindowListener
-dontwarn java.awt.font.FontRenderContext
-dontwarn java.awt.font.LineBreakMeasurer
-dontwarn java.awt.font.TextLayout
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Arc2D$Double
-dontwarn java.awt.geom.Path2D
-dontwarn java.awt.geom.Path2D$Double
-dontwarn java.awt.geom.Point2D
-dontwarn java.awt.geom.Point2D$Double
-dontwarn java.awt.geom.Rectangle2D$Double
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.RenderedImage
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.ImageReader
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.mail.Address
-dontwarn javax.mail.Authenticator
-dontwarn javax.mail.BodyPart
-dontwarn javax.mail.Message
-dontwarn javax.mail.Message$RecipientType
-dontwarn javax.mail.Multipart
-dontwarn javax.mail.PasswordAuthentication
-dontwarn javax.mail.Session
-dontwarn javax.mail.Transport
-dontwarn javax.mail.internet.AddressException
-dontwarn javax.mail.internet.InternetAddress
-dontwarn javax.mail.internet.MimeBodyPart
-dontwarn javax.mail.internet.MimeMessage
-dontwarn javax.mail.internet.MimeMultipart
-dontwarn javax.management.InstanceNotFoundException
-dontwarn javax.management.MBeanRegistrationException
-dontwarn javax.management.MBeanServer
-dontwarn javax.management.MalformedObjectNameException
-dontwarn javax.management.ObjectInstance
-dontwarn javax.management.ObjectName
-dontwarn javax.naming.Context
-dontwarn javax.naming.InitialContext
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn javax.script.AbstractScriptEngine
-dontwarn javax.script.Bindings
-dontwarn javax.script.Compilable
-dontwarn javax.script.CompiledScript
-dontwarn javax.script.Invocable
-dontwarn javax.script.ScriptContext
-dontwarn javax.script.ScriptEngine
-dontwarn javax.script.ScriptEngineFactory
-dontwarn javax.script.ScriptEngineManager
-dontwarn javax.script.ScriptException
-dontwarn javax.script.SimpleBindings
-dontwarn javax.servlet.Filter
-dontwarn javax.servlet.FilterChain
-dontwarn javax.servlet.FilterConfig
-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn javax.servlet.ServletContext
-dontwarn javax.servlet.ServletContextEvent
-dontwarn javax.servlet.ServletContextListener
-dontwarn javax.servlet.ServletException
-dontwarn javax.servlet.ServletRequest
-dontwarn javax.servlet.ServletResponse
-dontwarn javax.servlet.http.HttpServlet
-dontwarn javax.servlet.http.HttpServletRequest
-dontwarn javax.servlet.http.HttpServletResponse
-dontwarn javax.swing.AbstractButton
-dontwarn javax.swing.BorderFactory
-dontwarn javax.swing.Box
-dontwarn javax.swing.BoxLayout
-dontwarn javax.swing.ButtonGroup
-dontwarn javax.swing.CellEditor
-dontwarn javax.swing.DefaultListModel
-dontwarn javax.swing.DefaultListSelectionModel
-dontwarn javax.swing.DesktopManager
-dontwarn javax.swing.Icon
-dontwarn javax.swing.JButton
-dontwarn javax.swing.JCheckBoxMenuItem
-dontwarn javax.swing.JComboBox
-dontwarn javax.swing.JComponent
-dontwarn javax.swing.JDesktopPane
-dontwarn javax.swing.JDialog
-dontwarn javax.swing.JEditorPane
-dontwarn javax.swing.JFileChooser
-dontwarn javax.swing.JFrame
-dontwarn javax.swing.JInternalFrame
-dontwarn javax.swing.JLabel
-dontwarn javax.swing.JList
-dontwarn javax.swing.JMenu
-dontwarn javax.swing.JMenuBar
-dontwarn javax.swing.JMenuItem
-dontwarn javax.swing.JOptionPane
-dontwarn javax.swing.JPanel
-dontwarn javax.swing.JPopupMenu
-dontwarn javax.swing.JRadioButtonMenuItem
-dontwarn javax.swing.JRootPane
-dontwarn javax.swing.JScrollPane
-dontwarn javax.swing.JSplitPane
-dontwarn javax.swing.JTabbedPane
-dontwarn javax.swing.JTable
-dontwarn javax.swing.JTextArea
-dontwarn javax.swing.JTextPane
-dontwarn javax.swing.JToolBar
-dontwarn javax.swing.JTree
-dontwarn javax.swing.JViewport
-dontwarn javax.swing.KeyStroke
-dontwarn javax.swing.ListModel
-dontwarn javax.swing.ListSelectionModel
-dontwarn javax.swing.LookAndFeel
-dontwarn javax.swing.SwingUtilities
-dontwarn javax.swing.UIManager
-dontwarn javax.swing.border.Border
-dontwarn javax.swing.event.CellEditorListener
-dontwarn javax.swing.event.ChangeEvent
-dontwarn javax.swing.event.DocumentEvent
-dontwarn javax.swing.event.DocumentListener
-dontwarn javax.swing.event.EventListenerList
-dontwarn javax.swing.event.InternalFrameAdapter
-dontwarn javax.swing.event.InternalFrameEvent
-dontwarn javax.swing.event.InternalFrameListener
-dontwarn javax.swing.event.ListDataEvent
-dontwarn javax.swing.event.ListDataListener
-dontwarn javax.swing.event.ListSelectionEvent
-dontwarn javax.swing.event.ListSelectionListener
-dontwarn javax.swing.event.PopupMenuEvent
-dontwarn javax.swing.event.PopupMenuListener
-dontwarn javax.swing.event.TreeExpansionEvent
-dontwarn javax.swing.event.TreeExpansionListener
-dontwarn javax.swing.event.TreeModelEvent
-dontwarn javax.swing.event.TreeModelListener
-dontwarn javax.swing.filechooser.FileFilter
-dontwarn javax.swing.table.AbstractTableModel
-dontwarn javax.swing.table.TableCellEditor
-dontwarn javax.swing.table.TableCellRenderer
-dontwarn javax.swing.table.TableModel
-dontwarn javax.swing.text.BadLocationException
-dontwarn javax.swing.text.Caret
-dontwarn javax.swing.text.Document
-dontwarn javax.swing.text.JTextComponent
-dontwarn javax.swing.text.Segment
-dontwarn javax.swing.tree.DefaultTreeCellRenderer
-dontwarn javax.swing.tree.DefaultTreeSelectionModel
-dontwarn javax.swing.tree.TreeCellRenderer
-dontwarn javax.swing.tree.TreeModel
-dontwarn javax.swing.tree.TreePath
-dontwarn javax.swing.tree.TreeSelectionModel
-dontwarn org.apache.avalon.framework.logger.Logger
-dontwarn org.apache.bsf.BSFManager
-dontwarn org.apache.log.Hierarchy
-dontwarn org.apache.log.Logger
-dontwarn org.apache.xml.resolver.Catalog
-dontwarn org.apache.xml.resolver.CatalogManager
-dontwarn org.apache.xml.resolver.readers.CatalogReader
-dontwarn org.apache.xml.resolver.readers.SAXCatalogReader
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.codehaus.commons.compiler.CompileException
-dontwarn org.codehaus.groovy.control.CompilationFailedException
-dontwarn org.codehaus.groovy.control.CompilerConfiguration
-dontwarn org.codehaus.groovy.control.customizers.ImportCustomizer
-dontwarn org.codehaus.groovy.reflection.ClassInfo
-dontwarn org.codehaus.groovy.runtime.ArrayUtil
-dontwarn org.codehaus.groovy.runtime.BytecodeInterface8
-dontwarn org.codehaus.groovy.runtime.GStringImpl
-dontwarn org.codehaus.groovy.runtime.GeneratedClosure
-dontwarn org.codehaus.groovy.runtime.ScriptBytecodeAdapter
-dontwarn org.codehaus.groovy.runtime.callsite.CallSite
-dontwarn org.codehaus.groovy.runtime.callsite.CallSiteArray
-dontwarn org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation
-dontwarn org.codehaus.groovy.runtime.typehandling.ShortTypeHandling
-dontwarn org.codehaus.groovy.runtime.wrappers.Wrapper
-dontwarn org.codehaus.groovy.transform.ImmutableASTTransformation
-dontwarn org.codehaus.janino.ClassBodyEvaluator
-dontwarn org.codehaus.janino.ScriptEvaluator
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.eclipse.jetty.client.HttpClient
-dontwarn org.eclipse.jetty.io.ByteBufferPool
-dontwarn org.eclipse.jetty.util.ssl.SslContextFactory
-dontwarn org.eclipse.jetty.websocket.api.RemoteEndpoint
-dontwarn org.eclipse.jetty.websocket.api.Session
-dontwarn org.eclipse.jetty.websocket.api.WebSocketAdapter
-dontwarn org.eclipse.jetty.websocket.api.WebSocketPolicy
-dontwarn org.eclipse.jetty.websocket.client.WebSocketClient
-dontwarn org.graalvm.nativeimage.ImageSingletons
-dontwarn org.graalvm.nativeimage.hosted.Feature
-dontwarn org.graalvm.nativeimage.hosted.Feature$BeforeAnalysisAccess
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn org.joda.time.Instant
-dontwarn org.objectweb.asm.AnnotationVisitor
-dontwarn org.objectweb.asm.Attribute
-dontwarn org.objectweb.asm.ClassReader
-dontwarn org.objectweb.asm.ClassVisitor
-dontwarn org.objectweb.asm.FieldVisitor
-dontwarn org.objectweb.asm.Label
-dontwarn org.objectweb.asm.MethodVisitor
-dontwarn org.objectweb.asm.Type
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.tukaani.xz.ARMOptions
-dontwarn org.tukaani.xz.ARMThumbOptions
-dontwarn org.tukaani.xz.DeltaOptions
-dontwarn org.tukaani.xz.FilterOptions
-dontwarn org.tukaani.xz.FinishableOutputStream
-dontwarn org.tukaani.xz.FinishableWrapperOutputStream
-dontwarn org.tukaani.xz.IA64Options
-dontwarn org.tukaani.xz.LZMA2InputStream
-dontwarn org.tukaani.xz.LZMA2Options
-dontwarn org.tukaani.xz.LZMAInputStream
-dontwarn org.tukaani.xz.LZMAOutputStream
-dontwarn org.tukaani.xz.MemoryLimitException
-dontwarn org.tukaani.xz.PowerPCOptions
-dontwarn org.tukaani.xz.SPARCOptions
-dontwarn org.tukaani.xz.SingleXZInputStream
-dontwarn org.tukaani.xz.UnsupportedOptionsException
-dontwarn org.tukaani.xz.X86Options
-dontwarn org.tukaani.xz.XZ
-dontwarn org.tukaani.xz.XZInputStream
-dontwarn org.tukaani.xz.XZOutputStream
-dontwarn sun.reflect.Reflection

-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Cronet embedded
-dontwarn org.chromium.net.ThreadStatsUid
-dontwarn org.chromium.net.impl.CallbackExceptionImpl
-dontwarn org.chromium.net.impl.CronetEngineBase
-dontwarn org.chromium.net.impl.CronetEngineBuilderImpl$Pkp
-dontwarn org.chromium.net.impl.CronetEngineBuilderImpl$QuicHint
-dontwarn org.chromium.net.impl.CronetEngineBuilderImpl
-dontwarn org.chromium.net.impl.CronetExceptionImpl
-dontwarn org.chromium.net.impl.CronetLogger$CronetEngineBuilderInfo
-dontwarn org.chromium.net.impl.CronetLogger$CronetSource
-dontwarn org.chromium.net.impl.CronetLogger$CronetTrafficInfo
-dontwarn org.chromium.net.impl.CronetLogger$CronetVersion
-dontwarn org.chromium.net.impl.CronetLogger
-dontwarn org.chromium.net.impl.CronetLoggerFactory
-dontwarn org.chromium.net.impl.ImplVersion
-dontwarn org.chromium.net.impl.NetworkExceptionImpl
-dontwarn org.chromium.net.impl.Preconditions
-dontwarn org.chromium.net.impl.QuicExceptionImpl
-dontwarn org.chromium.net.impl.RequestFinishedInfoImpl
-dontwarn org.chromium.net.impl.UrlRequestBase
-dontwarn org.chromium.net.impl.UrlResponseInfoImpl$HeaderBlockImpl
-dontwarn org.chromium.net.impl.UrlResponseInfoImpl
-dontwarn org.chromium.net.impl.UserAgent
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$BidirectionalStreamCallback
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$LibraryLoader
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$NetworkQualityRttListenerWrapper
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$NetworkQualityThroughputListenerWrapper
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$RequestFinishedInfoListener
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$UploadDataProviderWrapper
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$UrlRequestCallback
-dontwarn org.chromium.net.impl.VersionSafeCallbacks$UrlRequestStatusListener

# For export zip
-keep class org.apache.commons.compress.archivers.zip.** { *; }
