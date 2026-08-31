(function(root,factory){
  var api=factory();
  if(typeof module==='object'&&module.exports)module.exports=api;
  root.BrigadeOrganization=api;
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';

  var BRIGADES=[
    {id:'longsha',name:'龙沙大队',account:'lsdd',stations:['安顺路消防站','海河路特勤站','永安街消防站','海山路消防站']},
    {id:'tiefeng',name:'铁锋大队',account:'tfdd',stations:['南树园消防站','红旗路消防站']},
    {id:'jianhua',name:'建华大队',account:'jhdd',stations:['华溪消防站','通北路消防站']},
    {id:'fularji',name:'富拉尔基大队',account:'fqdd',stations:['光明路消防站','宝石消防站']},
    {id:'angangxi',name:'昂昂溪大队',account:'aaxdd',stations:['新兴消防站']},
    {id:'meilisi',name:'梅里斯大队',account:'mlsdd',stations:['胜利消防站']},
    {id:'nianzishan',name:'碾子山大队',account:'nzsdd',stations:['向华路消防站']},
    {id:'longjiang',name:'龙江大队',account:'ljdd',stations:['新明路消防站']},
    {id:'yian',name:'依安大队',account:'yadd',stations:['学府路消防站']},
    {id:'tailai',name:'泰来大队',account:'tldd',stations:['建设路消防站']},
    {id:'gannan',name:'甘南大队',account:'gndd',stations:['奋斗街消防站']},
    {id:'fuyu',name:'富裕大队',account:'fydd',stations:['府右街消防站']},
    {id:'keshan',name:'克山大队',account:'ksdd',stations:['金鼎消防站']},
    {id:'nehe',name:'讷河大队',account:'nhdd',stations:['新和街消防站']},
    {id:'kedong',name:'克东大队',account:'kddd',stations:['惠民街消防站']},
    {id:'baiquan',name:'拜泉大队',account:'bqdd',stations:['南城路消防站']}
  ];
  var DIRECT_STATIONS=['战勤保障分队'];

  /* INCIDENT 2026-07-25: 误删大队账号。大队账号为固定编制，禁止删除/清空。 */
  var NEVER_DELETE_BRIGADE_ACCOUNTS=true;
  Object.freeze(BRIGADES);
  BRIGADES.forEach(function(b){if(b&&b.stations)Object.freeze(b.stations);Object.freeze(b)});
  function assertBrigadeImmutable(op){
    if(NEVER_DELETE_BRIGADE_ACCOUNTS){
      console.error('[BrigadeOrganization] blocked',op,'— 大队账号禁止删除');
      throw new Error('大队账号禁止删除（'+op+'）');
    }
  }
  function deleteBrigade(){assertBrigadeImmutable('deleteBrigade')}
  function removeBrigade(){assertBrigadeImmutable('removeBrigade')}
  function clearBrigades(){assertBrigadeImmutable('clearBrigades')}

  function copyBrigade(item){
    if(!item)return null;
    return {id:item.id,name:item.name,account:item.account,password:item.account+'119',stations:item.stations.slice()};
  }
  function listBrigades(){return BRIGADES.map(copyBrigade)}
  function getBrigade(id){return copyBrigade(BRIGADES.find(function(item){return item.id===String(id||'')}))}
  function getBrigadeByAccount(username){
    username=String(username||'').trim().toLowerCase();
    return copyBrigade(BRIGADES.find(function(item){return item.account===username}))
  }
  function getBrigadeForStation(station){
    station=String(station||'').trim();
    return copyBrigade(BRIGADES.find(function(item){return item.stations.indexOf(station)>=0}))
  }
  function getVisibleStations(account,allStations){
    account=account||{};allStations=Array.isArray(allStations)?allStations.slice():[];
    if(account.role==='hq'||account.role==='developer')return allStations;
    if(account.role==='brigade'){
      var brigade=getBrigade(account.brigade)||getBrigadeByAccount(account.username);
      return brigade?brigade.stations.filter(function(station){return allStations.indexOf(station)>=0}):[];
    }
    if(account.role==='station')return allStations.indexOf(account.station)>=0?[account.station]:[];
    return [];
  }
  function canAccessStation(account,station,allStations){return getVisibleStations(account,allStations).indexOf(station)>=0}
  function isCommandAccount(account){return !!(account&&(account.role==='hq'||account.role==='brigade'))}
  function getRowsForBrigade(rows,brigadeId){
    var brigade=getBrigade(brigadeId),allowed=brigade?brigade.stations:[];
    return (Array.isArray(rows)?rows:[]).filter(function(row){return allowed.indexOf(row&&row.station)>=0});
  }
  function filterStationRowsForAccount(rows,account){
    rows=Array.isArray(rows)?rows:[];
    if(account&&account.role==='hq')return rows.slice();
    if(account&&account.role==='brigade')return getRowsForBrigade(rows,account.brigade||(getBrigadeByAccount(account.username)||{}).id);
    return [];
  }
  function makeBrigadeDispatchCards(rows,account){
    return filterStationRowsForAccount(rows,account).filter(function(row){
      return row&&row.mode==='dispatch'&&Number(row.total)>0;
    });
  }
  function addTotals(target,row){
    ['groups','total','pending','in','warn','danger','out'].forEach(function(key){target[key]=(target[key]||0)+(Number(row&&row[key])||0)});
    target.updatedAt=Math.max(target.updatedAt||0,Number(row&&row.updatedAt)||0);
  }
  function makeHQDispatchCards(rows){
    rows=Array.isArray(rows)?rows:[];
    var cards=[];
    DIRECT_STATIONS.forEach(function(station){rows.filter(function(row){return row&&row.station===station}).forEach(function(row){cards.push(Object.assign({type:'station',name:station},row))})});
    BRIGADES.forEach(function(brigade){
      var memberRows=getRowsForBrigade(rows,brigade.id).filter(function(row){return row&&row.mode==='dispatch'&&Number(row.total)>0});
      if(!memberRows.length)return;
      var card={type:'brigade',brigadeId:brigade.id,name:brigade.name,stationCount:memberRows.length,groups:0,total:0,pending:0,in:0,warn:0,danger:0,out:0,updatedAt:0};
      memberRows.forEach(function(row){addTotals(card,row)});cards.push(card);
    });
    return cards.sort(function(a,b){return (b.updatedAt||0)-(a.updatedAt||0)});
  }

  return {
    DIRECT_STATIONS:DIRECT_STATIONS.slice(),listBrigades:listBrigades,getBrigade:getBrigade,
    getBrigadeByAccount:getBrigadeByAccount,getBrigadeForStation:getBrigadeForStation,
    getVisibleStations:getVisibleStations,canAccessStation:canAccessStation,isCommandAccount:isCommandAccount,
    getRowsForBrigade:getRowsForBrigade,filterStationRowsForAccount:filterStationRowsForAccount,
    makeBrigadeDispatchCards:makeBrigadeDispatchCards,makeHQDispatchCards:makeHQDispatchCards,
    deleteBrigade:deleteBrigade,removeBrigade:removeBrigade,clearBrigades:clearBrigades,
    NEVER_DELETE_BRIGADE_ACCOUNTS:true
  };
});
