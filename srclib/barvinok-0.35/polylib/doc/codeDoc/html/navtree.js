var NAVTREE =
[
  [ "polylib", "index.html", [
    [ "Class List", "annotated.html", [
      [ "_enode", "struct__enode.html", null ],
      [ "_enumeration", "struct__enumeration.html", null ],
      [ "_evalue", "struct__evalue.html", null ],
      [ "_Param_Domain", "struct__Param__Domain.html", null ],
      [ "_Param_Polyhedron", "struct__Param__Polyhedron.html", null ],
      [ "_Param_Vertex", "struct__Param__Vertex.html", null ],
      [ "_Polyhedron_union", "struct__Polyhedron__union.html", null ],
      [ "Ehrhart", "classEhrhart.html", null ],
      [ "factor", "structfactor.html", null ],
      [ "forsimplify", "structforsimplify.html", null ],
      [ "interval", "structinterval.html", null ],
      [ "LatticeUnion", "structLatticeUnion.html", null ],
      [ "linear_exception_holder", "structlinear__exception__holder.html", null ],
      [ "matrix", "structmatrix.html", null ],
      [ "polyhedron", "structpolyhedron.html", null ],
      [ "SatMatrix", "structSatMatrix.html", null ],
      [ "Vector", "structVector.html", null ],
      [ "ZPolyhedron", "structZPolyhedron.html", null ]
    ] ],
    [ "Class Index", "classes.html", null ],
    [ "Class Members", "functions.html", null ],
    [ "File List", "files.html", [
      [ "alpha.c", "alpha_8c.html", null ],
      [ "alpha.h", "alpha_8h.html", null ],
      [ "arithmetic_errors.h", "arithmetic__errors_8h.html", null ],
      [ "arithmetique.h", "arithmetique_8h.html", null ],
      [ "assert.h", "assert_8h.html", null ],
      [ "c2p.c", "c2p_8c.html", null ],
      [ "compress_parms.c", "compress__parms_8c.html", null ],
      [ "compress_parms.h", "compress__parms_8h.html", null ],
      [ "disjoint_union_adj.c", "disjoint__union__adj_8c.html", null ],
      [ "disjoint_union_sep.c", "disjoint__union__sep_8c.html", null ],
      [ "ehrhart.c", "ehrhart_8c.html", null ],
      [ "ehrhart.h", "ehrhart_8h.html", null ],
      [ "ehrhart_lower_bound.c", "ehrhart__lower__bound_8c.html", null ],
      [ "ehrhart_quick_apx.c", "ehrhart__quick__apx_8c.html", null ],
      [ "ehrhart_ranking.c", "ehrhart__ranking_8c.html", null ],
      [ "ehrhart_union.c", "ehrhart__union_8c.html", null ],
      [ "ehrhart_upper_bound.c", "ehrhart__upper__bound_8c.html", null ],
      [ "errormsg.c", "errormsg_8c.html", null ],
      [ "errormsg.h", "errormsg_8h.html", null ],
      [ "errors.c", "errors_8c.html", null ],
      [ "eval_ehrhart.c", "eval__ehrhart_8c.html", null ],
      [ "eval_ehrhart.h", "eval__ehrhart_8h.html", null ],
      [ "example.c", "example_8c.html", null ],
      [ "ext_ehrhart.c", "ext__ehrhart_8c.html", null ],
      [ "ext_ehrhart.h", "ext__ehrhart_8h.html", null ],
      [ "findv.c", "findv_8c.html", null ],
      [ "homogenization.c", "homogenization_8c.html", null ],
      [ "homogenization.h", "homogenization_8h.html", null ],
      [ "Lattice.c", "Lattice_8c.html", null ],
      [ "Lattice.h", "Lattice_8h.html", null ],
      [ "Matop.c", "Matop_8c.html", null ],
      [ "Matop.h", "Matop_8h.html", null ],
      [ "matrix.c", "matrix_8c.html", null ],
      [ "matrix.h", "matrix_8h.html", null ],
      [ "matrix_addon.c", "matrix__addon_8c.html", null ],
      [ "matrix_addon.h", "matrix__addon_8h.html", null ],
      [ "matrix_permutations.c", "matrix__permutations_8c.html", null ],
      [ "matrix_permutations.h", "matrix__permutations_8h.html", null ],
      [ "NormalForms.c", "NormalForms_8c.html", null ],
      [ "NormalForms.h", "NormalForms_8h.html", null ],
      [ "param.c", "param_8c.html", null ],
      [ "param.h", "param_8h.html", null ],
      [ "polyhedron.c", "polyhedron_8c.html", null ],
      [ "polyhedron.h", "polyhedron_8h.html", null ],
      [ "polylib32.h", "polylib32_8h.html", null ],
      [ "polylib64.h", "polylib64_8h.html", null ],
      [ "polylibgmp.h", "polylibgmp_8h.html", null ],
      [ "polyparam.c", "polyparam_8c.html", null ],
      [ "polyparam.h", "polyparam_8h.html", null ],
      [ "polytest.c", "polytest_8c.html", null ],
      [ "pp.c", "pp_8c.html", null ],
      [ "r2p.c", "r2p_8c.html", null ],
      [ "ranking.c", "ranking_8c.html", null ],
      [ "ranking.h", "ranking_8h.html", null ],
      [ "SolveDio.c", "SolveDio_8c.html", null ],
      [ "SolveDio.h", "SolveDio_8h.html", null ],
      [ "testCompressParms.c", "testCompressParms_8c.html", null ],
      [ "testehrhart.c", "testehrhart_8c.html", null ],
      [ "testlib.c", "testlib_8c.html", null ],
      [ "types.h", "types_8h.html", null ],
      [ "vector.c", "vector_8c.html", null ],
      [ "vector.h", "vector_8h.html", null ],
      [ "verif_ehrhart.c", "verif__ehrhart_8c.html", null ],
      [ "Zpolyhedron.c", "Zpolyhedron_8c.html", null ],
      [ "Zpolyhedron.h", "Zpolyhedron_8h.html", null ],
      [ "Zpolytest.c", "Zpolytest_8c.html", null ]
    ] ],
    [ "File Members", "globals.html", null ]
  ] ]
];

function createIndent(o,domNode,node,level)
{
  if (node.parentNode && node.parentNode.parentNode)
  {
    createIndent(o,domNode,node.parentNode,level+1);
  }
  var imgNode = document.createElement("img");
  if (level==0 && node.childrenData)
  {
    node.plus_img = imgNode;
    node.expandToggle = document.createElement("a");
    node.expandToggle.href = "javascript:void(0)";
    node.expandToggle.onclick = function() 
    {
      if (node.expanded) 
      {
        $(node.getChildrenUL()).slideUp("fast");
        if (node.isLast)
        {
          node.plus_img.src = node.relpath+"ftv2plastnode.png";
        }
        else
        {
          node.plus_img.src = node.relpath+"ftv2pnode.png";
        }
        node.expanded = false;
      } 
      else 
      {
        expandNode(o, node, false);
      }
    }
    node.expandToggle.appendChild(imgNode);
    domNode.appendChild(node.expandToggle);
  }
  else
  {
    domNode.appendChild(imgNode);
  }
  if (level==0)
  {
    if (node.isLast)
    {
      if (node.childrenData)
      {
        imgNode.src = node.relpath+"ftv2plastnode.png";
      }
      else
      {
        imgNode.src = node.relpath+"ftv2lastnode.png";
        domNode.appendChild(imgNode);
      }
    }
    else
    {
      if (node.childrenData)
      {
        imgNode.src = node.relpath+"ftv2pnode.png";
      }
      else
      {
        imgNode.src = node.relpath+"ftv2node.png";
        domNode.appendChild(imgNode);
      }
    }
  }
  else
  {
    if (node.isLast)
    {
      imgNode.src = node.relpath+"ftv2blank.png";
    }
    else
    {
      imgNode.src = node.relpath+"ftv2vertline.png";
    }
  }
  imgNode.border = "0";
}

function newNode(o, po, text, link, childrenData, lastNode)
{
  var node = new Object();
  node.children = Array();
  node.childrenData = childrenData;
  node.depth = po.depth + 1;
  node.relpath = po.relpath;
  node.isLast = lastNode;

  node.li = document.createElement("li");
  po.getChildrenUL().appendChild(node.li);
  node.parentNode = po;

  node.itemDiv = document.createElement("div");
  node.itemDiv.className = "item";

  node.labelSpan = document.createElement("span");
  node.labelSpan.className = "label";

  createIndent(o,node.itemDiv,node,0);
  node.itemDiv.appendChild(node.labelSpan);
  node.li.appendChild(node.itemDiv);

  var a = document.createElement("a");
  node.labelSpan.appendChild(a);
  node.label = document.createTextNode(text);
  a.appendChild(node.label);
  if (link) 
  {
    a.href = node.relpath+link;
  } 
  else 
  {
    if (childrenData != null) 
    {
      a.className = "nolink";
      a.href = "javascript:void(0)";
      a.onclick = node.expandToggle.onclick;
      node.expanded = false;
    }
  }

  node.childrenUL = null;
  node.getChildrenUL = function() 
  {
    if (!node.childrenUL) 
    {
      node.childrenUL = document.createElement("ul");
      node.childrenUL.className = "children_ul";
      node.childrenUL.style.display = "none";
      node.li.appendChild(node.childrenUL);
    }
    return node.childrenUL;
  };

  return node;
}

function showRoot()
{
  var headerHeight = $("#top").height();
  var footerHeight = $("#nav-path").height();
  var windowHeight = $(window).height() - headerHeight - footerHeight;
  navtree.scrollTo('#selected',0,{offset:-windowHeight/2});
}

function expandNode(o, node, imm)
{
  if (node.childrenData && !node.expanded) 
  {
    if (!node.childrenVisited) 
    {
      getNode(o, node);
    }
    if (imm)
    {
      $(node.getChildrenUL()).show();
    } 
    else 
    {
      $(node.getChildrenUL()).slideDown("fast",showRoot);
    }
    if (node.isLast)
    {
      node.plus_img.src = node.relpath+"ftv2mlastnode.png";
    }
    else
    {
      node.plus_img.src = node.relpath+"ftv2mnode.png";
    }
    node.expanded = true;
  }
}

function getNode(o, po)
{
  po.childrenVisited = true;
  var l = po.childrenData.length-1;
  for (var i in po.childrenData) 
  {
    var nodeData = po.childrenData[i];
    po.children[i] = newNode(o, po, nodeData[0], nodeData[1], nodeData[2],
        i==l);
  }
}

function findNavTreePage(url, data)
{
  var nodes = data;
  var result = null;
  for (var i in nodes) 
  {
    var d = nodes[i];
    if (d[1] == url) 
    {
      return new Array(i);
    }
    else if (d[2] != null) // array of children
    {
      result = findNavTreePage(url, d[2]);
      if (result != null) 
      {
        return (new Array(i).concat(result));
      }
    }
  }
  return null;
}

function initNavTree(toroot,relpath)
{
  var o = new Object();
  o.toroot = toroot;
  o.node = new Object();
  o.node.li = document.getElementById("nav-tree-contents");
  o.node.childrenData = NAVTREE;
  o.node.children = new Array();
  o.node.childrenUL = document.createElement("ul");
  o.node.getChildrenUL = function() { return o.node.childrenUL; };
  o.node.li.appendChild(o.node.childrenUL);
  o.node.depth = 0;
  o.node.relpath = relpath;

  getNode(o, o.node);

  o.breadcrumbs = findNavTreePage(toroot, NAVTREE);
  if (o.breadcrumbs == null)
  {
    o.breadcrumbs = findNavTreePage("index.html",NAVTREE);
  }
  if (o.breadcrumbs != null && o.breadcrumbs.length>0)
  {
    var p = o.node;
    for (var i in o.breadcrumbs) 
    {
      var j = o.breadcrumbs[i];
      p = p.children[j];
      expandNode(o,p,true);
    }
    p.itemDiv.className = p.itemDiv.className + " selected";
    p.itemDiv.id = "selected";
    $(window).load(showRoot);
  }
}

