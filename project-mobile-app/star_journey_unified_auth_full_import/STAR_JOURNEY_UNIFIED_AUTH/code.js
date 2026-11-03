figma.showUI(__html__,{width:420,height:410});
const regular={family:'Inter',style:'Regular'},heavy={family:'Inter',style:'Bold'};
const hex=h=>{let q=h.replace('#','');return {r:parseInt(q.slice(0,2),16)/255,g:parseInt(q.slice(2,4),16)/255,b:parseInt(q.slice(4,6),16)/255}};
const solid=h=>[{type:'SOLID',color:hex(h)}];
const NAV=['Học','Ôn tập','Tiến độ','Xếp hạng','Hồ sơ'];
const TARGET=['home','review','progress','ranking','profile'];
const ICONS=['✦','◇','↗','★','◉'];
const PARENTS={register:'welcome',forgot_password:'welcome',reset_sent:'forgot_password',reset_password:'reset_sent',reset_complete:'welcome',onboarding_band:'welcome',onboarding_topics:'onboarding_band',onboarding_name:'onboarding_topics',catalog:'home',topic_detail:'catalog',lesson_detail:'topic_detail',vocab_1:'lesson_detail',listening:'vocab_7',listening_result:'listening',writing:'listening_result',lesson_complete:'writing',writing_feedback:'lesson_complete',writing_revision:'writing_feedback',review_detail:'review',review_vocab:'review_detail',review_listening:'review_vocab',review_writing:'review_listening',review_result:'review_writing',activity:'progress',lesson_history:'progress',lesson_history_detail:'lesson_history',report_name:'ranking',profile_edit:'profile',preferences:'profile',notification_settings:'profile',account_settings:'profile',admin_topics:'profile',admin_editor:'admin_topics'};
for(let i=2;i<=7;i++)PARENTS['vocab_'+i]='vocab_'+(i-1);
let P,links=[],screens={},issues=[],numLinks=0;
function text(v,size=16,weight=false,c='#FFFFFF',w=324){const n=figma.createText();n.fontName=weight?heavy:regular;n.characters=String(v);n.fontSize=size;n.fills=solid(c);n.resize(w,Math.max(25,size*1.4));n.textAutoResize='HEIGHT';return n;}
function frame(name,w,h,c,r=0){const f=figma.createFrame();f.name=name;f.resize(w,h);f.fills=solid(c);f.cornerRadius=r;return f;}
function stack(name,w,c,gap=10,pad=16){let f=frame(name,w,60,c);f.layoutMode='VERTICAL';f.primaryAxisSizingMode='AUTO';f.counterAxisSizingMode='FIXED';f.itemSpacing=gap;f.paddingLeft=pad;f.paddingRight=pad;f.paddingTop=pad;f.paddingBottom=pad;return f;}
function addStroke(f,c,width=1){f.strokes=[{type:'SOLID',color:hex(c)}];f.strokeWeight=width;}
function decorated(base,w,rad=20){const f=stack('EDITABLE · '+base,w,P.cardSurface||P.surface,10,18);f.cornerRadius=rad;addStroke(f,P.cardOutline||P.outline);return f;}
function appendText(f,str,size=15,bold=false,c=P.text){if(!String(str||'').trim())return;const readableSize=(!bold&&size===13)?14:size;f.appendChild(text(str,readableSize,bold,c,Math.max(100,f.width-f.paddingLeft-f.paddingRight)));}
function action(b,from,source){if(b.route)links.push({source,from,to:b.route,label:b.title});}
function makeBlock(b,w,id){const ac=P[b.accent]||P.mint;let f;
 switch(b.type){
 case 'hero':f=stack('HERO · '+b.title,w,P.background,5,4);appendText(f,b.title,27,true,ac);if(b.subtitle)appendText(f,b.subtitle,13,false,P.muted);break;
 case 'section':f=stack('SECTION · '+b.title,w,P.background,1,4);appendText(f,b.title.toUpperCase(),13,true,P.muted);break;
 case 'button':{
 const ghost=b.appearance==='ghost';
 const label=b.title+(ghost?'':'  →');
 const buttonWidth=Math.min(w,Math.max(146,Math.ceil(Array.from(label).length*8.6+36)));
 f=stack('BUTTON clickable · '+b.title,buttonWidth,ghost?P.background:ac,4,14);
 if(ghost)f.fills=[];
 f.cornerRadius=14;f.paddingTop=12;f.paddingBottom=12;
 const caption=text(label,15,true,ghost?P.mint:P.ink,buttonWidth-28);caption.textAlignHorizontal='CENTER';f.appendChild(caption);
 break;
 }
 case 'field':f=decorated('FIELD · '+b.title,w,13);appendText(f,b.title,13,true,P.muted);appendText(f,b.hint||'Nhập thông tin',15,false,P.text);if(id==='writing'||id==='writing_revision'||id==='review_writing')f.paddingBottom=52;break;
 case 'journey':{
 f=stack('JOURNEY path · '+b.title,w,P.background,12,15);appendText(f,'┃ '+b.title,24,true,ac);appendText(f,b.body||'',13,false,P.muted);
 const nodes=frame('CURVED JOURNEY VISUAL',w-30,206,P.background,12);f.appendChild(nodes);
 const positions=[[42,130],[140,72],[235,20]];
 for(let i=0;i<3;i++){const [x,y]=positions[i];let orb=figma.createEllipse();orb.resize(i===2?104:57,i===2?104:57);orb.x=x;orb.y=y;orb.fills=solid(i===2?P.yellow:P.surface2);orb.strokes=[{type:'SOLID',color:hex(i===2?P.yellow:P.outline)}];orb.strokeWeight=3;nodes.appendChild(orb);const ch=text(i===2?'✦':'●',i===2?34:20,true,i===2?P.ink:P.mint,45);ch.x=x+(i===2?33:15);ch.y=y+(i===2?27:12);nodes.appendChild(ch);}
 if(b.route)appendText(f,(b.cta||'XEM BÀI HỌC')+'  →',17,true,P.yellow);break;
 }
 case 'stats':{
 f=decorated('DAILY FLOW · '+b.title,w,22);appendText(f,'✦  '+b.title,19,true,P.text);
 const parts=(b.body||'').split(' • ').filter(Boolean);for(const part of parts)appendText(f,'◈  '+part,17,true,P.text);break;
 }
 case 'streak':f=decorated('STREAK · '+b.title,w,24);appendText(f,'♨    '+b.title,29,true,P.yellow);appendText(f,'●   ○   ○   ○   ○   ○   ○',21,true,P.muted);appendText(f,b.body||'',13,false,P.muted);break;
 case 'heatmap':{
 f=decorated('ACTIVITY HEATMAP · '+b.title,w,22);appendText(f,'▦   '+b.title,20,true,P.text);appendText(f,b.body||'',12,false,P.muted);
 for(let r=0;r<5;r++){const row=frame('HEATMAP row '+r,w-40,32,P.surface);f.appendChild(row);for(let col=0;col<7;col++){const tile=frame('Day '+(r*7+col+1),31,26,((r*7+col)%11===0)?P.mint:((r*7+col)%7===0?P.surface2:P.indigo),5);row.appendChild(tile);tile.x=col*40;tile.y=2;}}break;
 }
 case 'podium':f=decorated('LEADERBOARD PODIUM',w,24);appendText(f,'♛   BẢNG VINH DANH',19,true,P.yellow);appendText(f,'🥈       🥇       🥉',31,true,P.text);appendText(f,'  #2         #1         #3',19,true,P.text);appendText(f,b.body||'',13,false,P.muted);break;
 case 'vocab_progress':{
 const position=Number(b.position),total=Number(b.total);
 f=frame('VOCAB POSITION · '+position+'/'+total,w,52,P.background);f.fills=[];
 const label=text('TỪ '+position+' TRONG '+total,12,true,P.mint,220);f.appendChild(label);label.x=4;label.y=1;
 const gap=7,segmentWidth=Math.min(42,(w-8-gap*(total-1))/total);
 for(let i=0;i<total;i++){const segment=frame('Từ '+(i+1)+(i===position-1?' · đang học':i<position-1?' · đã qua':' · tiếp theo'),segmentWidth,6,i===position-1?P.yellow:i<position-1?P.mint:(P.cardOutline||P.outline),3);f.appendChild(segment);segment.x=4+i*(segmentWidth+gap);segment.y=30;}
 break;
 }
 case 'progress':f=decorated('REVIEW MILESTONES · '+b.title,w,20);appendText(f,b.title,17,true,P.text);appendText(f,'● ━━━ ● ━━━ ● ━━━ ○',15,true,P.mint);appendText(f,String(b.value||'')+'  '+(b.body||''),12,false,P.muted);break;
 case 'segment':{
 if(!Array.isArray(b.options)||!b.options.length){f=decorated('FILTER · '+b.title,w,14);appendText(f,b.title,14,true,P.text);break;}
 f=frame('FILTER · '+b.title,w,56,P.cardSurface||P.surface,14);addStroke(f,P.cardOutline||P.outline);
 const gap=5,pad=5,itemW=(w-pad*2-gap*(b.options.length-1))/b.options.length;
 b.options.forEach((option,i)=>{const selected=!!option.selected;const item=frame('FILTER OPTION · '+option.label,itemW,46,selected?P.mint:(P.cardSurface||P.surface),10);f.appendChild(item);item.x=pad+i*(itemW+gap);item.y=5;const label=text(option.label,15,true,selected?P.ink:P.text,itemW-12);label.textAlignHorizontal='CENTER';item.appendChild(label);label.x=6;label.y=11;if(option.route&&!selected)links.push({source:item,from:id,to:option.route,label:option.label});});
 break;
 }
 case 'chips':f=decorated('VOCABULARY CHIPS',w,17);(b.items||[]).forEach(t=>appendText(f,'✓  '+t,14,false,P.text));break;
 case 'steps':f=stack('STEPS',w,P.background,4,6);appendText(f,(b.items||[]).join('   →   '),12,true,P.mint);break;
 case 'celebrate':f=decorated('LESSON COMPLETED',w,24);appendText(f,'✓   '+b.title,26,true,P.mint);appendText(f,b.body||'',14,false,P.text);break;
 case 'profile':f=decorated('PROFILE',w,24);appendText(f,'◉   '+b.title,20,true,P.text);appendText(f,b.body||'',13,false,P.muted);break;
 case 'banner':f=decorated('BANNER · '+b.title,w,19);appendText(f,'✦  '+b.title,18,true,ac);appendText(f,b.body||'',13,false,P.muted);break;
 default:f=decorated('CARD · '+b.title,w,19);if(id==='listening'&&b.title==='Trình phát audio'){f.fills=solid(P.elevatedSurface||P.surface);addStroke(f,P.mint,1.5);}if(id==='writing'&&b.title&&b.title.startsWith('Tình huống')){f.fills=solid(P.background);addStroke(f,P.cardOutline||P.outline,1);}if(b.tag)appendText(f,b.tag,11,true,ac);appendText(f,b.title,18,true,P.text);if(b.body)appendText(f,b.body,14,false,P.muted);if(b.route&&b.linkText!==false)appendText(f,'Xem chi tiết  →',12,true,ac);
 }
 action(b,id,f);return f;
}
function starfield(root,seed){const stars=frame('STARFIELD · fixed decorative layer',390,844,P.background);root.appendChild(stars);stars.x=0;stars.y=0;
 for(let i=0;i<37;i++){const x=(i*97+seed*41+53)%384;const y=(i*137+seed*29+17)%823;const d=(i%11===0)?6:((i%4===0)?4:2);const dot=figma.createEllipse();dot.name='Star '+i;dot.resize(d,d);dot.x=x;dot.y=y;dot.fills=solid((i%5===0)?P.mint:((i%3===0)?P.indigo:P.star));stars.appendChild(dot);}
}
async function reaction(source,to){if(!source||!to)return;await source.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:to.id,navigation:'NAVIGATE',transition:null,preserveScrollPosition:false}]}]);numLinks++;}

function authLabel(parent, str, x,y, w, size, color, bold){const n=text(str,size,!!bold,color,w);parent.appendChild(n);n.x=x;n.y=y;return n;}
function authPanel(parent,name,x,y,w,h,color,r=18){const n=frame(name,w,h,color,r);parent.appendChild(n);n.x=x;n.y=y;return n;}
function authButton(parent,label,x,y,w,h,bg,fg,route,outlined=false){
 const b=authPanel(parent,'ACTION · '+label,x,y,w,h,bg,h/2);
 if(outlined)addStroke(b,P.outline,1);
 const t=authLabel(b,label,0,0,w,14,fg,true);t.textAlignHorizontal='CENTER';t.y=(h-22)/2;
 if(route)links.push({source:b,from:'welcome',to:route,label:label});return b;
}
function makeAuthScreen(){
 const root=frame('welcome · CHÀO MỪNG + ĐĂNG NHẬP (UNIFIED)',390,844,P.background,24);root.clipsContent=true;screens.welcome=root;
 const bg=frame('STAR SKY BACKGROUND',390,844,P.background);root.appendChild(bg);
 for(let i=0;i<50;i++){const dot=figma.createEllipse();let radius=i%9===0?4:2;dot.resize(radius,radius);dot.x=(i*83+19)%382;dot.y=(i*127+31)%822;dot.fills=solid(i%5===0?P.mint:P.star);bg.appendChild(dot);}
 authLabel(root,'9:41',22,12,80,12,P.muted,true);
 authLabel(root,'●  ▰',330,12,52,12,P.muted,true);
 // Visual hero, built from editable native Figma frames rather than a flattened screenshot.
 const hero=authPanel(root,'ILLUSTRATED HERO · EDITABLE',20,56,350,274,P.surface,28);
 addStroke(hero,P.outline,1);
 const halo=figma.createEllipse();halo.name='Hero ambient halo';halo.resize(210,210);halo.x=70;halo.y=12;halo.fills=solid('#404D9A');hero.appendChild(halo);
 const phone=authPanel(hero,'MINI LEARNING PHONE',100,18,150,244,'#161B3D',28);addStroke(phone,P.mint,3);
 authPanel(phone,'Dynamic island',52,8,46,11,'#080B18',7);
 authLabel(phone,'✦ DAILY FLOW',17,32,119,12,P.mint,true);
 authPanel(phone,'Progress track',16,65,116,9,P.surface2,6);
 authPanel(phone,'Progress completed',16,65,73,9,P.yellow,6);
 const coin=figma.createEllipse();coin.name='Lesson path coin';coin.resize(76,76);coin.x=37;coin.y=95;coin.fills=solid(P.yellow);phone.appendChild(coin);
 authLabel(phone,'✦',59,110,43,33,P.ink,true);
 authLabel(phone,'BÀI 01',38,182,93,17,P.text,true);
 authLabel(phone,'Học từ · Nghe · Viết',22,208,124,10,P.muted,false);
 const bubble=authPanel(hero,'VOCAB REWARD PILL',13,112,117,43,'#FFFFFF',21);
 authLabel(bubble,'✓  7 từ mới',8,10,101,14,'#20264B',true);
 const badge=authPanel(hero,'REVISION BADGE',233,136,104,50,P.mint,18);
 authLabel(badge,'✓  Ôn tập',7,14,95,13,P.ink,true);
 authLabel(root,'HỌC MỖI NGÀY,',39,350,312,29,P.text,true).textAlignHorizontal='CENTER';
 authLabel(root,'TIẾN BỘ MỖI NGÀY',20,384,350,26,P.mint,true).textAlignHorizontal='CENTER';
 authLabel(root,'Từ vựng theo chủ đề · Luyện nghe · Luyện viết',25,426,340,12,P.muted,false).textAlignHorizontal='CENTER';
 // Compact sign-in form, separate affordances, matching the airy example composition.
 const mail=authPanel(root,'FIELD username · editable',32,460,326,54,'#22294F',15);addStroke(mail,P.outline,1);
 authLabel(mail,'TÊN ĐĂNG NHẬP',16,7,280,11,P.muted,true);
 authLabel(mail,'Nhập tên đăng nhập',16,27,296,14,P.text,false);
 const pass=authPanel(root,'FIELD password · editable',32,523,326,54,'#22294F',15);addStroke(pass,P.outline,1);
 authLabel(pass,'MẬT KHẨU',16,7,230,11,P.muted,true);
 authLabel(pass,'••••••••',16,27,230,14,P.text,false);
 const visibility=authPanel(pass,'ACTION · Hiện mật khẩu',270,3,48,48,P.background,12);visibility.fills=[];
 authLabel(visibility,'Hiện',2,16,44,12,P.mint,false);
 authButton(root,'ĐĂNG NHẬP',85,590,220,48,P.mint,P.ink,'home');
 authLabel(root,'Chưa có tài khoản?',55,659,174,13,P.muted,false);
 authButton(root,'Tạo tài khoản',210,647,150,48,P.background,P.mint,'register');
 authButton(root,'Quên mật khẩu?',105,705,180,48,P.background,P.muted,'forgot_password');
 authLabel(root,'Tiếp tục nghĩa là bạn đồng ý với',48,766,294,13,P.muted,false).textAlignHorizontal='CENTER';
 authLabel(root,'Điều khoản dịch vụ & Chính sách bảo mật',40,789,310,13,P.text,false).textAlignHorizontal='CENTER';
 return root;
}

function makeScreen(d,k){if(d.id==='welcome')return makeAuthScreen();if(d.parent)PARENTS[d.id]=d.parent;const w=390,h=844;const root=frame((d.screenKind==='response_state'?'STATE · ':'')+d.id+' · '+d.title,w,h,P.background,24);root.x=(k%7)*455;root.y=Math.floor(k/7)*925;root.clipsContent=true;starfield(root,k);screens[d.id]=root;
 const isTab=!!d.tab;const header=frame('FIXED status bar',390,45,P.background);root.appendChild(header);header.y=0;const status=text('9:41                                    ●  ▰',11,true,P.muted,354);header.appendChild(status);status.x=20;status.y=13;
 const top=frame('FIXED header / BACK',390,55,P.background);root.appendChild(top);top.y=45;
 if(PARENTS[d.id]){const b=frame('BACK button ←',104,48,P.background,12);b.fills=[];top.appendChild(b);b.x=15;b.y=3;const bt=text('← Quay lại',14,true,P.mint,96);b.appendChild(bt);bt.x=4;bt.y=11;links.push({source:b,from:d.id,to:PARENTS[d.id],label:'Quay lại'});}
 else if(isTab){const t=text(d.title,23,true,P.text,310);top.appendChild(t);t.x=20;t.y=8;}
 const vp=frame('VERTICAL SCROLL VIEWPORT',390,isTab?653:718,P.background);vp.clipsContent=true;vp.y=100;root.appendChild(vp);
 try{vp.overflowDirection='VERTICAL';}catch(e){}
 // Starfield repeated within the viewport for rich cohesive visual even when scrolling.
 for(let j=0;j<10;j++){let dot=figma.createEllipse();dot.name='Scroll star';dot.resize(3,3);dot.fills=solid(P.star);vp.appendChild(dot);dot.x=(j*73+23)%381;dot.y=(j*61+9)%600;}
 const content=stack('EDITABLE SCROLL CONTENT',390,P.background,13,18);vp.appendChild(content);content.x=0;content.y=0;
 if(!isTab){appendText(content,d.title,25,true,P.text);}if(d.subtitle)appendText(content,d.subtitle,13,false,P.muted);
 for(const b of d.blocks||[])content.appendChild(makeBlock(b,354,d.id));
 if(isTab){const nav=frame('FIXED BOTTOM NAVIGATION',390,91,P.background);root.appendChild(nav);nav.y=753;addStroke(nav,P.outline,1);
 NAV.forEach((label,j)=>{const active=label===d.tab;const button=frame('NAV '+label+(active?' · selected':''),74,78,P.background,10);nav.appendChild(button);button.x=2+j*77;button.y=5;const v=text(ICONS[j]+'\n'+label,13,active,active?P.yellow:P.muted,70);v.setRangeFontSize(0,1,20);v.textAlignHorizontal='CENTER';v.resize(70,52);button.appendChild(v);v.x=2;v.y=12;if(!active)links.push({source:button,from:d.id,to:TARGET[j],label:'Tab '+label});});}
 return root;
}
async function run(doc){if(!doc||doc.format!=='vocab-figma-import-v1'||!Array.isArray(doc.screens)||doc.screens.length<1)throw Error('Invalid design.json format');if(doc.meta?.screenCount!==doc.screens.length)throw Error('screenCount mismatch');const ids=new Set(doc.screens.map(s=>s.id));if(ids.size!==doc.screens.length)throw Error('Duplicate screen ID');for(const s of doc.screens){for(const b of s.blocks||[]){if(b.route&&!ids.has(b.route))throw Error('Missing route '+s.id+' → '+b.route);for(const o of b.options||[])if(o.route&&!ids.has(o.route))throw Error('Missing filter route '+s.id+' → '+o.route);}}P=Object.assign({star:'#5979C6',outline:'#5D6DAB'},doc.theme);links=[];screens={};issues=[];numLinks=0;
 const page=figma.createPage();page.name='STAR JOURNEY · UI/UX REFINEMENT · '+doc.screens.length+' SCREENS';await figma.setCurrentPageAsync(page);
 for(let i=0;i<doc.screens.length;i++)makeScreen(doc.screens[i],i);
 for(const l of links){try{if(!screens[l.to]){issues.push('No target: '+l.from+' → '+l.to);continue;}if(l.from===l.to)continue;await reaction(l.source,screens[l.to]);}catch(e){issues.push(l.from+' → '+l.to+': '+String(e));}}
 try{if(typeof page.setFlowStartingPointsAsync==='function'&&screens.welcome)await page.setFlowStartingPointsAsync([{nodeId:screens.welcome.id,name:'Start Star Journey'}]);}catch(e){issues.push('Flow start: '+String(e));}
 await figma.setCurrentPageAsync(page);figma.viewport.scrollAndZoomIntoView(page.children.slice(0,7));
 figma.ui.postMessage({type:'result',screens:doc.screens.length,links:numLinks,issues:issues.slice(0,30)});figma.notify('Star Journey unified auth: '+doc.screens.length+' screens, '+numLinks+' prototype links.');}
figma.ui.onmessage=async m=>{if(m.type!=='import')return;try{await Promise.all([figma.loadFontAsync(regular),figma.loadFontAsync(heavy)]);await run(JSON.parse(m.json));}catch(e){figma.ui.postMessage({type:'error',error:String(e)});}};
