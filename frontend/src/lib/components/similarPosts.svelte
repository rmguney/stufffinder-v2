<script>
  import { writable } from 'svelte/store';
  import { onMount, onDestroy, afterUpdate } from 'svelte';
  import { threadStore } from '../../threadStore'; // Import threadStore
  import { PUBLIC_API_URL } from "$env/static/public"; // Needed for tag fetching

  // Props for the carousel
  export let thread = {}; // Current thread object
  export let tagDetails = writable({}); // Accept the store itself

  // Track previous thread ID to detect navigation changes
  let previousThreadId = null;
  
  // Internal state for calculated similar posts
  let internalSimilarPosts = [];

  // Enhanced carousel control with proper reference 
  let similarPosts = [];
  
  // Track container width changes more effectively with debouncing
  let carouselContainer;
  let resizeTimeout;
  let currentSlide = 0;
  let touchStartX = 0;
  let touchEndX = 0;
  let visibleSlides = 3;
  let containerWidth;

  // Enhanced loading state for semantic relationships
  let loadingSemantic = true;
  let loadingSubclassRelations = false;
  let postsWithSemanticInfo = [];
  let subclassCache = new Map(); // Cache for subclass relationships to avoid redundant queries
  let commonParentCache = new Map(); // New cache for common parent relationships (siblings)
  let relationshipExplanations = new Map(); // Cache for relationship explanations

  // Domain-agnostic semantic linking ruleset with hierarchical relationship types
  const relationTypes = {
    // Hierarchical relationships - Increase P279 weight significantly for higher priority
    'P31': { name: 'INSTANCE_OF', weight: 0.8, hierarchy: 'child' },
    'P279': { name: 'SUBCLASS_OF', weight: 1.2, hierarchy: 'child' }, // Significantly increased weight for P279
    'P171': { name: 'PARENT_TAXON', weight: 0.85, hierarchy: 'child' },
    // Compositional relationships  
    'P361': { name: 'PART_OF', weight: 0.7, hierarchy: 'child' },
    'P527': { name: 'HAS_PART', weight: 0.7, hierarchy: 'parent' },
    // Other significant relationships
    'P1889': { name: 'DIFFERENT_FROM', weight: 0.3, hierarchy: 'sibling' },
    'P460': { name: 'SAME_AS', weight: 1.0, hierarchy: 'sibling' },
    'P1382': { name: 'COINCIDENT_WITH', weight: 0.85, hierarchy: 'sibling' },
    'P366': { name: 'USED_FOR', weight: 0.6, hierarchy: 'function' },
    'P1535': { name: 'USED_BY', weight: 0.6, hierarchy: 'function' },
    'P1552': { name: 'HAS_QUALITY', weight: 0.75, hierarchy: 'attribute' },
    'P186': { name: 'MATERIAL', weight: 0.8, hierarchy: 'composition' },
    'P137': { name: 'OPERATOR', weight: 0.5, hierarchy: 'role' },
    'P462': { name: 'COLOR', weight: 0.6, hierarchy: 'attribute' },
  };

  // Simplified relation visualization options with more basic icons
  const relationVisuals = {
    'direct': { 
      icon: 'M9 5l7 7-7 7', 
      color: 'text-teal-600 dark:text-teal-400',
      badge: 'bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border-teal-200 dark:border-teal-800/50'
    },
    'subclass': { 
      icon: 'M19 9l-7 7-7-7', 
      color: 'text-blue-600 dark:text-blue-400',
      badge: 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-800/50'
    },
    'superclass': { 
      icon: 'M5 15l7-7 7 7', 
      color: 'text-indigo-600 dark:text-indigo-400',
      badge: 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800/50'
    },
    'sibling': { 
      icon: 'M8 7h8M8 12h8M8 17h8', 
      color: 'text-purple-600 dark:text-purple-400',
      badge: 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300 border-purple-200 dark:border-purple-800/50'
    },
    'common_parent': { 
      icon: 'M7 8l5-5 5 5M7 16l5 5 5-5', 
      color: 'text-green-600 dark:text-green-400',
      badge: 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300 border-green-200 dark:border-green-800/50'
    },
    'transitive': { 
      icon: 'M5 12h14', 
      color: 'text-amber-600 dark:text-amber-400',
      badge: 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-800/50'
    },
    'composition': { 
      icon: 'M3 6l9 6 9-6M3 12l9 6 9-6', 
      color: 'text-rose-600 dark:text-rose-400',
      badge: 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-300 border-rose-200 dark:border-rose-800/50'
    }
  };

  // Enhanced implementation of semantic similarity scoring with hierarchical bonuses
  function calculateSimilarityScore(post, currentThread) {
    let score = 0;
    let scoreDetails = [];
    
    // Base score from matching tag count
    if (post.matchingTagCount > 0) {
      // Use an exponential formula to give higher rewards for multiple matching tags
      const tagScore = Math.min(65, 30 * (1 + 0.5 * (post.matchingTagCount - 1)));
      score += tagScore;
      scoreDetails.push({
        factor: 'Tags', 
        points: Math.round(tagScore),
        explanation: `${post.matchingTagCount} matching tag${post.matchingTagCount !== 1 ? 's' : ''}`
      });
    }
    
    // Significant bonus for subclass relationship
    if (post.hasSubclassRelation) {
      const subclassScore = 30; // Increased from 20 to 30
      score += subclassScore;
      scoreDetails.push({
        factor: 'Subclass', 
        points: subclassScore,
        explanation: 'Taxonomic relationship between concepts'
      });
    }
    
    // New: Bonus for common parent class (siblings)
    if (post.hasCommonParentRelation) {
      const siblingScore = 25;
      score += siblingScore;
      scoreDetails.push({
        factor: 'Common Parent', 
        points: siblingScore,
        explanation: 'Items share a common parent class'
      });
    }

    // New: Bonus for transitive relationship through subclass
    if (post.hasTransitiveRelation) {
      const transitiveScore = 22;
      score += transitiveScore;
      scoreDetails.push({
        factor: 'Transitive', 
        points: transitiveScore,
        explanation: 'Connected through relationship chain'
      });
    }
    
    // Add points for each semantic relationship found
    if (post.semanticLinks && post.semanticLinks.length > 0) {
      post.semanticLinks.forEach(link => {
        const relationWeight = link.relationWeight || 0.5;
        const hopPenalty = 0.7 ** (link.hops - 1); // Penalty for indirect links
        
        // Extra bonus for P279 relationships
        const isSubclassRelation = link.relationType === 'SUBCLASS_OF';
        const subclassBonus = isSubclassRelation ? 1.8 : 1.0; // 80% bonus for subclass relations (increased from 50%)
        
        // Add hierarchical relationship bonuses
        let hierarchyBonus = 1.0;
        if (link.hierarchyType) {
          switch(link.hierarchyType) {
            case 'parent': hierarchyBonus = 1.3; break; // Increased from 1.2
            case 'child': hierarchyBonus = 1.2; break; // Increased from 1.1
            case 'sibling': hierarchyBonus = 1.4; break; // Increased from 1.3
            case 'cousin': hierarchyBonus = 1.0; break;
            case 'uncle': hierarchyBonus = 0.95; break; 
            default: hierarchyBonus = 1.0;
          }
        }
        
        const linkPoints = 25 * relationWeight * hopPenalty * hierarchyBonus * subclassBonus;
        score += linkPoints;
        
        // Only add detailed score for top relationships to avoid clutter
        if (linkPoints > 10) {
          scoreDetails.push({
            factor: link.relationType,
            points: Math.round(linkPoints),
            explanation: `${link.hops}-hop ${link.relationType.toLowerCase()} relation`
          });
        }
      });
    }
    
    // Normalize score to 0-100 range and round to nearest integer
    return {
      score: Math.min(100, Math.round(score)),
      details: scoreDetails
    };
  }

  // Special function to check if two entities are related via P279 (subclass of) chain
  async function checkSubclassRelation(sourceEntityId, targetEntityId) {
    // Check cache first
    const cacheKey = `${sourceEntityId}-${targetEntityId}`;
    const reverseCacheKey = `${targetEntityId}-${sourceEntityId}`;
    
    if (subclassCache.has(cacheKey)) {
      return subclassCache.get(cacheKey);
    }
    if (subclassCache.has(reverseCacheKey)) {
      return subclassCache.get(reverseCacheKey);
    }
    
    try {
      // Specific SPARQL query focused on P279 (subclass of) relationship up to 3 levels deep
      const sparqlQuery = `
        ASK {
          {
            # Direct subclass relationship
            wd:${sourceEntityId} wdt:P279 wd:${targetEntityId} .
          } UNION {
            # Target is a subclass of source
            wd:${targetEntityId} wdt:P279 wd:${sourceEntityId} .
          } UNION {
            # Indirect (2-hop) subclass relationship
            wd:${sourceEntityId} wdt:P279/wdt:P279 wd:${targetEntityId} .
          } UNION {
            # Reverse indirect (2-hop)
            wd:${targetEntityId} wdt:P279/wdt:P279 wd:${sourceEntityId} .
          } UNION {
            # Common parent class (siblings)
            wd:${sourceEntityId} wdt:P279 ?commonParent .
            wd:${targetEntityId} wdt:P279 ?commonParent .
            FILTER(?sourceEntityId != ?targetEntityId)
          } UNION {
            # Indirect (3-hop) relationship
            wd:${sourceEntityId} wdt:P279/wdt:P279/wdt:P279 wd:${targetEntityId} .
          } UNION {
            # Reverse indirect (3-hop)
            wd:${targetEntityId} wdt:P279/wdt:P279/wdt:P279 wd:${sourceEntityId} .
          }
        }
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout
      
      const response = await fetch(url, { signal: controller.signal });
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        console.error('SPARQL subclass query failed:', response.status);
        subclassCache.set(cacheKey, false);
        return false;
      }
      
      const data = await response.json();
      const hasSubclassRelation = data.boolean === true;
      
      // Store in cache
      subclassCache.set(cacheKey, hasSubclassRelation);
      
      return hasSubclassRelation;
    } catch (error) {
      if (error.name === 'AbortError') {
        console.warn('SPARQL subclass query timed out for', sourceEntityId, targetEntityId);
      } else {
        console.error('Error in SPARQL subclass query:', error);
      }
      subclassCache.set(cacheKey, false);
      return false;
    }
  }

  // New function to check if two entities share a common parent class
  async function checkCommonParentClass(sourceEntityId, targetEntityId) {
    // Check cache first with both key orientations
    const cacheKey = `${sourceEntityId}-${targetEntityId}-commonParent`;
    const reverseCacheKey = `${targetEntityId}-${sourceEntityId}-commonParent`;
    
    if (commonParentCache.has(cacheKey)) {
      return commonParentCache.get(cacheKey);
    }
    if (commonParentCache.has(reverseCacheKey)) {
      return commonParentCache.get(reverseCacheKey);
    }
    
    try {
      // Enhanced SPARQL query to find if two entities share a common parent class
      // Using a more robust query that works bidirectionally
      const sparqlQuery = `
        SELECT DISTINCT ?commonParent ?commonParentLabel WHERE {
          VALUES ?entity1 { wd:${sourceEntityId} }
          VALUES ?entity2 { wd:${targetEntityId} }
          
          # Find common parent class through SUBCLASS_OF
          {
            ?entity1 wdt:P279 ?commonParent .
            ?entity2 wdt:P279 ?commonParent .
            FILTER(?entity1 != ?entity2)
          }
          UNION
          # Also try with P31 (instance of)
          {
            ?entity1 wdt:P31 ?commonParent .
            ?entity2 wdt:P31 ?commonParent .
            FILTER(?entity1 != ?entity2)
          }
          UNION
          # Try one level up in the hierarchy
          {
            ?entity1 wdt:P279/wdt:P279 ?commonParent .
            ?entity2 wdt:P279 ?commonParent .
            FILTER(?entity1 != ?entity2)
          }
          UNION
          {
            ?entity1 wdt:P279 ?commonParent .
            ?entity2 wdt:P279/wdt:P279 ?commonParent .
            FILTER(?entity1 != ?entity2)
          }
          
          # Get label for common parent
          SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
        LIMIT 5
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5-second timeout
      
      const response = await fetch(url, { signal: controller.signal });
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        console.error('Common parent SPARQL query failed:', response.status);
        commonParentCache.set(cacheKey, { hasCommonParent: false });
        return { hasCommonParent: false };
      }
      
      const data = await response.json();
      
      if (!data.results || !data.results.bindings || data.results.bindings.length === 0) {
        commonParentCache.set(cacheKey, { hasCommonParent: false });
        return { hasCommonParent: false };
      }
      
      // Get common parents with their labels
      const commonParents = data.results.bindings.map(binding => {
        const parentUri = binding.commonParent?.value;
        const parentId = parentUri ? parentUri.split('/').pop() : '';
        return {
          id: parentId,
          label: binding.commonParentLabel?.value || parentId
        };
      });
      
      const result = {
        hasCommonParent: true,
        commonParents
      };
      
      // Store in both directional caches for consistency
      commonParentCache.set(cacheKey, result);
      commonParentCache.set(reverseCacheKey, result); // Store the reverse key too for consistency
      
      return result;
    } catch (error) {
      if (error.name === 'AbortError') {
        console.warn('Common parent SPARQL query timed out for', sourceEntityId, targetEntityId);
      } else {
        console.error('Error in common parent SPARQL query:', error);
      }
      commonParentCache.set(cacheKey, { hasCommonParent: false });
      commonParentCache.set(reverseCacheKey, { hasCommonParent: false }); // Store the reverse key too
      return { hasCommonParent: false };
    }
  }

  // New function to check for transitive relationships through subclasses
  async function checkTransitiveRelations(sourceEntityId, targetEntityId) {
    try {
      // SPARQL query to find transitive relationships (A→B→C)
      const sparqlQuery = `
        SELECT ?intermediateEntity ?intermediateLabel ?relation ?relationLabel WHERE {
          VALUES ?source { wd:${sourceEntityId} }
          VALUES ?target { wd:${targetEntityId} }
          
          # Find relations where source relates to something that is a subclass of target
          # OR target relates to something that is a subclass of source
          {
            # Pattern: Source --relation--> Intermediate --subclass--> Target
            ?source ?relation ?intermediateEntity .
            ?intermediateEntity wdt:P279 ?target .
          } UNION {
            # Pattern: Target --relation--> Intermediate --subclass--> Source
            ?target ?relation ?intermediateEntity .
            ?intermediateEntity wdt:P279 ?source .
          } UNION {
            # Pattern: Source --subclass--> Intermediate --relation--> Target
            ?source wdt:P279 ?intermediateEntity .
            ?intermediateEntity ?relation ?target .
          } UNION {
            # Pattern: Target --subclass--> Intermediate --relation--> Source
            ?target wdt:P279 ?intermediateEntity .
            ?intermediateEntity ?relation ?source .
          }
          
          FILTER(?relation != wdt:P279) # Exclude direct subclass relations
          
          # Get labels
          SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
        LIMIT 5
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      const response = await fetch(url, { signal: controller.signal });
      clearTimeout(timeoutId);
      
      if (!response.ok) {
        console.error('Transitive relation query failed:', response.status);
        return { hasTransitiveRelation: false };
      }
      
      const data = await response.json();
      
      if (!data.results || !data.results.bindings || data.results.bindings.length === 0) {
        return { hasTransitiveRelation: false };
      }
      
      // Extract transitive relationships
      const transitiveRelations = data.results.bindings.map(binding => {
        const relUri = binding.relation?.value;
        const relId = relUri ? relUri.split('/').pop() : '';
        const intermediateUri = binding.intermediateEntity?.value;
        const intermediateId = intermediateUri ? intermediateUri.split('/').pop() : '';
        
        return {
          relationId: relId,
          relationLabel: binding.relationLabel?.value || 'related to',
          intermediateEntityId: intermediateId,
          intermediateEntityLabel: binding.intermediateLabel?.value || intermediateId
        };
      });
      
      return {
        hasTransitiveRelation: true,
        transitiveRelations
      };
    } catch (error) {
      console.error('Error in transitive relation query:', error);
      return { hasTransitiveRelation: false };
    }
  }

  // Enhanced query to find semantic connections with hierarchical relationships
  async function fetchSemanticLinks(sourceEntityId, targetEntityId) {
    try {
      // Enhanced SPARQL query to prioritize subclass relationships
      const sparqlQuery = `
        SELECT ?source ?target ?relation ?relationLabel ?hierarchy ?hops ?hop1 ?hop1Label ?hop2 ?hop2Label WHERE {
          VALUES ?source { wd:${sourceEntityId} }
          VALUES ?target { wd:${targetEntityId} }
          
          # Direct relationship (1 hop) - prioritizing P279 even more strongly
          {
            # Check P279 (subclass of) first for direct relationship
            {
              ?source wdt:P279 ?target .
              BIND(wdt:P279 AS ?relation)
              BIND("child" AS ?hierarchy)
              BIND(1 AS ?hops)
            } UNION {
              ?target wdt:P279 ?source .
              BIND(wdt:P279 AS ?relation)
              BIND("parent" AS ?hierarchy)
              BIND(1 AS ?hops)
            } UNION {
              # Then check other relations with lower priority
              ?source ?relation ?target .
              FILTER(?relation != wdt:P279)
              BIND(1 AS ?hops)
              
              # Determine hierarchy type for direct relationships
              OPTIONAL {
                # Check if source is a parent of target
                {
                  VALUES ?parentRel { wdt:P279 wdt:P31 wdt:P171 }
                  ?target ?parentRel ?source .
                  BIND("parent" AS ?hierarchy)
                } UNION {
                  # Check if source is a child of target
                  VALUES ?childRel { wdt:P279 wdt:P31 wdt:P171 }
                  ?source ?childRel ?target .
                  BIND("child" AS ?hierarchy)
                } UNION {
                  # Check if source and target are siblings (same parent)
                  VALUES ?siblingRel { wdt:P279 wdt:P31 wdt:P171 }
                  ?source ?siblingRel ?commonParent .
                  ?target ?siblingRel ?commonParent .
                  FILTER(?source != ?target)
                  BIND("sibling" AS ?hierarchy)
                }
              }
            }
          }
          UNION
          # 2-hop path with hierarchy identification - prioritize P279 chains even more
          {
            # First check specifically for P279 chains (subclass hierarchy)
            {
              ?source wdt:P279 ?hop1 .
              ?hop1 wdt:P279 ?target .
              BIND(wdt:P279 AS ?relation)
              BIND("child" AS ?hierarchy)
              BIND(2 AS ?hops)
            } UNION {
              ?target wdt:P279 ?hop1 .
              ?hop1 wdt:P279 ?source .
              BIND(wdt:P279 AS ?relation)
              BIND("parent" AS ?hierarchy)
              BIND(2 AS ?hops)
            } UNION {
              # Common parent class (siblings) - new priority case
              ?source wdt:P279 ?hop1 .
              ?target wdt:P279 ?hop1 .
              FILTER(?source != ?target)
              BIND(wdt:P279 AS ?relation)
              BIND("sibling" AS ?hierarchy)
              BIND(2 AS ?hops)
            } UNION {
              # Then check other 2-hop paths
              ?source ?relation ?hop1 .
              ?hop1 ?relation2 ?target .
              FILTER(?relation != wdt:P279 || ?relation2 != wdt:P279)
              BIND(2 AS ?hops)
              
              # Try to determine hierarchy type for 2-hop relationships
              OPTIONAL {
                # Check for grandparent relationship
                VALUES ?parentRel { wdt:P279 wdt:P31 wdt:P171 }
                ?hop1 ?parentRel ?source .
                ?target ?parentRel ?hop1 .
                BIND("grandparent" AS ?hierarchy)
              }
              OPTIONAL {
                # Check for grandchild relationship
                VALUES ?childRel { wdt:P279 wdt:P31 wdt:P171 }
                ?source ?childRel ?hop1 .
                ?hop1 ?childRel ?target .
                BIND("grandchild" AS ?hierarchy)
              }
              OPTIONAL {
                # Check for cousin relationship
                VALUES ?cousinRel { wdt:P279 wdt:P31 wdt:P171 }
                ?source ?cousinRel ?parentSource .
                ?target ?cousinRel ?parentTarget .
                ?parentSource wdt:P279 ?commonAncestor .
                ?parentTarget wdt:P279 ?commonAncestor .
                FILTER(?parentSource != ?parentTarget)
                BIND("cousin" AS ?hierarchy)
              }
              OPTIONAL {
                # Check for uncle/aunt relationship
                VALUES ?uncleRel { wdt:P279 wdt:P31 wdt:P171 }
                ?source ?uncleRel ?grandparent .
                ?parentTarget ?uncleRel ?grandparent .
                ?target ?uncleRel ?parentTarget .
                BIND("uncle" AS ?hierarchy)
              }
            }
          }
          
          SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
        LIMIT 20
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // Increased from 3s to 5s for complex queries
      
      try {
        const response = await fetch(url, { signal: controller.signal });
        clearTimeout(timeoutId);
        
        if (!response.ok) {
          console.error('SPARQL query failed:', response.status);
          return [];
        }
        
        const data = await response.json();
        
        if (!data.results || !data.results.bindings || data.results.bindings.length === 0) {
          return [];
        }
        
        // Process and return the results with hierarchy information
        return data.results.bindings.map(result => {
          const relUri = result.relation?.value;
          const relId = relUri ? relUri.split('/').pop() : '';
          const relInfo = relationTypes[relId] || { name: 'RELATED', weight: 0.5, hierarchy: null };
          
          // Determine hierarchy type (from SPARQL if available, fallback to relation default)
          let hierarchyType = result.hierarchy?.value || relInfo.hierarchy;
          
          // Give special weight to P279 relations
          const relationWeight = relId === 'P279' ? 1.2 : (relInfo.weight || 0.5);
          
          // Determine relationship visualization type
          let visualType = 'direct';
          if (relId === 'P279') {
            if (hierarchyType === 'child') visualType = 'subclass';
            else if (hierarchyType === 'parent') visualType = 'superclass';
            else if (hierarchyType === 'sibling') visualType = 'sibling';
          }
          else if (['P361', 'P527'].includes(relId)) {
            visualType = 'composition';
          }
          
          return {
            sourceEntity: sourceEntityId,
            targetEntity: targetEntityId,
            relationType: relInfo.name,
            relationId: relId,
            relationWeight: relationWeight,
            relationLabel: result.relationLabel?.value || 'related to',
            hops: result.hops?.value ? parseInt(result.hops.value) : 1,
            hierarchyType: hierarchyType,
            isSubclassRelation: relId === 'P279',
            visualType,
            intermediates: [
              result.hop1?.value ? { 
                id: result.hop1.value.split('/').pop(),
                label: result.hop1Label?.value 
              } : null,
              result.hop2?.value ? {
                id: result.hop2.value.split('/').pop(),
                label: result.hop2Label?.value
              } : null
            ].filter(Boolean)
          };
        });
      } catch (error) {
        if (error.name === 'AbortError') {
          console.warn('SPARQL query timed out for', sourceEntityId, targetEntityId);
        } else {
          console.error('Error in SPARQL query:', error);
        }
        return [];
      }
    } catch (error) {
      console.error('Error preparing semantic links query:', error);
      return [];
    }
  }

  // Function to fetch details for tags from Wikidata (moved/adapted from page)
  async function fetchTagDetailsForSimilar(tags) {
    if (!tags || !Array.isArray(tags) || tags.length === 0) return;

    try {
      let fetchedTagDetails = {};
      const currentDetails = $tagDetails; // Get current details to avoid re-fetching

      const tagsToFetch = tags.filter(qcode => qcode && !currentDetails[qcode]);
      if (tagsToFetch.length === 0) return; // All needed tags already fetched

      for (const qcode of tagsToFetch) {
        try {
          const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
          const data = await response.json();

          if (data.entities && data.entities[qcode]) {
            const entity = data.entities[qcode];
            fetchedTagDetails[qcode] = {
              id: qcode,
              label: entity.labels?.en?.value || 'Unknown label',
              description: entity.descriptions?.en?.value || 'No description'
            };
          }
        } catch (error) {
          console.error(`Failed to fetch details for tag ${qcode}:`, error);
        }
      }

      // Update the shared tagDetails store
      tagDetails.update(existing => ({...existing, ...fetchedTagDetails}));
    } catch (error) {
      console.error("Failed to fetch tag details for similar posts:", error);
    }
  }

  // Enhanced reactive calculation for similar posts
  $: {
    if (thread && thread.id && thread.tags && $threadStore) {
      // Find all posts (not just direct tag matches)
      internalSimilarPosts = $threadStore.filter(t => {
        // Don't include the current thread
        if (String(t.id) === String(thread.id)) return false;

        // Check if there are any tags at all (both posts must have tags)
        if (!thread.tags || !t.tags || thread.tags.length === 0 || t.tags.length === 0) return false;

        // Include all posts with tags for further semantic analysis
        return true;
      }).map(post => {
        // Calculate number of matching tags
        const matchingTagCount = post.tags.filter(tag => thread?.tags?.includes(tag)).length;
        return { ...post, matchingTagCount };
      });

      // Sort direct matches first, then by recency for potential semantic matches
      const directMatches = internalSimilarPosts.filter(post => post.matchingTagCount > 0);
      const potentialSemanticMatches = internalSimilarPosts.filter(post => post.matchingTagCount === 0);
      
      directMatches.sort((a, b) => b.matchingTagCount - a.matchingTagCount);
      potentialSemanticMatches.sort((a, b) => b.id - a.id); // Most recent first
      
      internalSimilarPosts = [...directMatches, ...potentialSemanticMatches.slice(0, 30)]; // Take up to 30 potential semantic matches
      
      // Reset slide when posts change
      currentSlide = 0;
    } else {
      internalSimilarPosts = [];
    }
  }

  // Reactive effect to fetch tag details for the calculated similar posts
  $: {
    if (internalSimilarPosts.length > 0) {
      const allSimilarTags = new Set();
      internalSimilarPosts.forEach(post => {
        if (post.tags) {
          post.tags.forEach(tag => allSimilarTags.add(tag));
        }
      });
      if (allSimilarTags.size > 0) {
        fetchTagDetailsForSimilar([...allSimilarTags]);
      }
    }
  }

  // Update visibleSlides and handle edge cases
  function updateVisibleSlides() {
    if (containerWidth < 500) visibleSlides = 1; 
    else if (containerWidth < 900) visibleSlides = 2;
    else visibleSlides = 3;
    
    // Ensure currentSlide doesn't exceed bounds
    const maxSlide = Math.max(0, similarPosts.length - visibleSlides);
    if (currentSlide > maxSlide) currentSlide = maxSlide;
  }

  // Completely rebuilt pagination system
  function calculatePagination() {
    if (!similarPosts || !similarPosts.length) {
      return {
        currentPage: 0,
        totalPages: 0,
        itemsPerPage: visibleSlides,
        startIndex: 0,
        endIndex: 0,
        hasNext: false,
        hasPrev: false
      };
    }
    
    // Calculate current page (0-indexed)
    const currentPage = Math.floor(currentSlide / visibleSlides);
    
    // Calculate total pages
    const totalPages = Math.ceil(similarPosts.length / visibleSlides);
    
    // Calculate start and end indices for current page
    const startIndex = currentPage * visibleSlides;
    const endIndex = Math.min(startIndex + visibleSlides, similarPosts.length);
    
    return {
      currentPage,
      totalPages,
      itemsPerPage: visibleSlides,
      startIndex,
      endIndex,
      hasNext: currentPage < totalPages - 1,
      hasPrev: currentPage > 0
    };
  }

  // Add this reactive statement to recalculate pagination
  $: pagination = calculatePagination();

  // Pagination-aware slide navigation
  function nextSlide() {
    if (pagination.hasNext) {
      currentSlide = Math.min(
        currentSlide + visibleSlides, 
        similarPosts.length - visibleSlides
      );
    }
  }

  function prevSlide() {
    if (pagination.hasPrev) {
      currentSlide = Math.max(0, currentSlide - visibleSlides);
    }
  }

  function goToPage(pageIndex) {
    if (pageIndex >= 0 && pageIndex < pagination.totalPages) {
      currentSlide = pageIndex * visibleSlides;
    }
  }

  // Improved touch handling with better thresholds 
  function handleTouchStart(e) {
    touchStartX = e.touches[0].clientX;
  }

  function handleTouchMove(e) {
    // Track move to improve responsiveness
    touchEndX = e.touches[0].clientX;
  }

  function handleTouchEnd(e) {
    touchEndX = e.changedTouches[0].clientX;
    handleSwipe();
  }

  function handleSwipe() {
    const swipeDistance = touchEndX - touchStartX;
    // Improved threshold based on screen width
    const threshold = Math.min(containerWidth * 0.1, 80);
    
    if (swipeDistance > threshold) {
      // Swipe right
      prevSlide();
    } else if (swipeDistance < -threshold) {
      // Swipe left
      nextSlide();
    }
  }

  // Function to handle window resize events
  function handleResize() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
      if (carouselContainer) {
        containerWidth = carouselContainer.offsetWidth;
        
        // Ensure currentSlide is valid after resize
        const maxSlide = Math.max(0, similarPosts.length - visibleSlides);
        if (currentSlide > maxSlide) currentSlide = maxSlide;
      }
    }, 100);
  }

  // Function to get a friendly display for relation types
  function getRelationDisplay(relationType) {
    switch(relationType) {
      case 'SUBCLASS_OF': return 'SUBCLASS'; // Changed to shorter form
      case 'INSTANCE_OF': return 'TYPE';
      case 'PART_OF': return 'PART OF';
      case 'HAS_PART': return 'CONTAINS';
      case 'SAME_AS': return 'SAME AS';
      case 'USED_FOR': return 'USED FOR';
      case 'MATERIAL': return 'MADE OF';
      case 'PARENT_TAXON': return 'PARENT';
      case 'COLOR': return 'COLOR';
      default: return 'RELATED';
    }
  }

  // Function to get a friendly display for hierarchy types
  function getHierarchyDisplay(hierarchyType) {
    switch(hierarchyType) {
      case 'parent': return 'Parent';
      case 'child': return 'Child';
      case 'sibling': return 'Sibling';
      case 'cousin': return 'Cousin';
      case 'grandparent': return 'Ancestor';
      case 'grandchild': return 'Descendant';
      case 'uncle': return 'Related Class';
      default: return '';
    }
  }

  // Function to get a color for relation types
  function getRelationColor(relationType) {
    switch(relationType) {
      case 'SUBCLASS_OF': return 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-800/50';
      case 'INSTANCE_OF': return 'bg-indigo-100 dark:bg-indigo-900/30 text-indigo-800 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800/50';
      case 'PART_OF': return 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-300 border-amber-200 dark:border-amber-800/50';
      case 'HAS_PART': return 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-300 border-orange-200 dark:border-orange-800/50';
      case 'SAME_AS': return 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800/50';
      case 'USED_FOR': return 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300 border-purple-200 dark:border-purple-800/50';
      case 'MATERIAL': return 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-300 border-rose-200 dark:border-rose-800/50';
      case 'PARENT_TAXON': return 'bg-cyan-100 dark:bg-cyan-900/30 text-cyan-800 dark:text-cyan-300 border-cyan-200 dark:border-cyan-800/50';
      case 'COLOR': return 'bg-fuchsia-100 dark:bg-fuchsia-900/30 text-fuchsia-800 dark:text-fuchsia-300 border-fuchsia-200 dark:border-fuchsia-800/50';
      default: return 'bg-neutral-100 dark:bg-neutral-900/30 text-neutral-800 dark:text-neutral-300 border-neutral-200 dark:border-neutral-800/50';
    }
  }

  // Function to get icon and color for hierarchy types
  function getHierarchyStyle(hierarchyType) {
    switch(hierarchyType) {
      case 'parent':
        return {
          icon: 'M5 10l7-7m0 0l7 7m-7-7v18',
          color: 'text-blue-600 dark:text-blue-400'
        };
      case 'child':
        return {
          icon: 'M19 14l-7 7m0 0l-7-7m7 7V3',
          color: 'text-purple-600 dark:text-purple-400'
        };
      case 'sibling':
        return {
          icon: 'M8 11h4m-2-2v4M3 12h1m8-9v1m8 8h1M5.6 5.6l.7.7m12.1-.7l-.7.7',
          color: 'text-green-600 dark:text-green-400'
        };
      case 'cousin':
        return {
          icon: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
          color: 'text-amber-600 dark:text-amber-400'
        };
      case 'grandparent':
        return {
          icon: 'M5 10l7-7m0 0l7 7m-7-7v18',
          color: 'text-indigo-600 dark:text-indigo-400'
        };
      case 'grandchild':
        return {
          icon: 'M19 14l-7 7m0 0l-7-7m7 7V3',
          color: 'text-rose-600 dark:text-rose-400'
        };
      case 'uncle':
        return {
          icon: 'M7 20l4-16m2 16l4-16M6 9h14M4 15h14',
          color: 'text-cyan-600 dark:text-cyan-400'
        };
      default:
        return {
          icon: 'M13 10V3L4 14h7v7l9-11h-7z',
          color: 'text-neutral-600 dark:text-neutral-400'
        };
    }
  }

  // Process semantic relationships when internalSimilarPosts changes
  $: if (internalSimilarPosts && internalSimilarPosts.length > 0 && thread && thread.tags) {
    processPosts();
  } else if (!internalSimilarPosts || internalSimilarPosts.length === 0) {
    // Ensure loading state is false and posts are cleared if no similar posts
    loadingSemantic = false;
    postsWithSemanticInfo = [];
  }

  // Enhanced process function with additional relationship types
  async function processPosts() {
    if (!thread?.tags || thread.tags.length === 0 || !internalSimilarPosts || internalSimilarPosts.length === 0) {
      loadingSemantic = false;
      postsWithSemanticInfo = [];
      return;
    }
    
    loadingSemantic = true;
    loadingSubclassRelations = true;
    
    try {
      // First process direct matches (posts with matching tags)
      const directMatches = internalSimilarPosts.filter(post => post.matchingTagCount > 0);
      
      // Process all posts for semantic relationships
      const allPosts = internalSimilarPosts;
      
      console.log(`Processing ${allPosts.length} posts for semantic relationships`);
      
      // Process posts for various relationship types
      const processedResults = await Promise.all(
        allPosts.map(async post => {
          try {
            const postCopy = { 
              ...post, 
              semanticLinks: [], 
              hasSubclassRelation: false,
              hasCommonParentRelation: false,
              hasTransitiveRelation: false,
              similarityScore: 0 
            };
            
            // Get up to 5 tags from each post for more comprehensive analysis
            const threadTags = thread.tags.slice(0, 5);
            const postTags = post.tags.slice(0, 5);
            
            // 1. First check for direct subclass relationships (highest priority)
            let subclassFound = false;
            let subclassLinks = [];
            
            for (const sourceTag of threadTags) {
              if (subclassFound) break;
              
              for (const targetTag of postTags) {
                // Check for direct subclass relationship
                const isSubclassRelated = await checkSubclassRelation(sourceTag, targetTag);
                
                if (isSubclassRelated) {
                  console.log(`Found subclass relation between ${sourceTag} and ${targetTag}`);
                  subclassFound = true;
                  
                  // Fetch details
                  const links = await fetchSemanticLinks(sourceTag, targetTag);
                  if (links && links.length > 0) {
                    subclassLinks.push(...links);
                    break;
                  }
                }
              }
            }
            
            // 2. Check for common parent classes (sibling relationship)
            let commonParentFound = false;
            let commonParentsInfo = null;
            
            if (!subclassFound) {
              // Use more combinations of source and target tags to increase detection chance
              const maxTagsToCheck = Math.min(5, Math.max(threadTags.length, postTags.length));
              
              for (let i = 0; i < maxTagsToCheck; i++) {
                if (commonParentFound) break;
                const sourceTag = threadTags[i % threadTags.length];
                
                for (let j = 0; j < maxTagsToCheck; j++) {
                  if (commonParentFound) break;
                  const targetTag = postTags[j % postTags.length];
                  
                  // Skip identical tags
                  if (sourceTag === targetTag) continue;
                  
                  // Check for common parent classes with debug logging
                  const result = await checkCommonParentClass(sourceTag, targetTag);
                  
                  if (result.hasCommonParent) {
                    commonParentFound = true;
                    commonParentsInfo = result;
                    break;
                  }
                }
              }
            }
            
            // 3. Check for transitive relationships through subclass chains
            let transitiveFound = false;
            let transitiveInfo = null;
            
            if (!subclassFound && !commonParentFound) {
              for (const sourceTag of threadTags) {
                if (transitiveFound) break;
                
                for (const targetTag of postTags) {
                  // Check for transitive relationships
                  const result = await checkTransitiveRelations(sourceTag, targetTag);
                  
                  if (result.hasTransitiveRelation) {
                    console.log(`Found transitive relation between ${sourceTag} and ${targetTag}`);
                    transitiveFound = true;
                    transitiveInfo = result;
                    break;
                  }
                }
              }
            }
            
            // 4. Finally look for any other semantic relationships
            let otherSemanticLinks = [];
            
            if (!subclassFound && !commonParentFound && !transitiveFound) {
              for (const sourceTag of threadTags) {
                for (const targetTag of postTags) {
                  try {
                    const links = await fetchSemanticLinks(sourceTag, targetTag);
                    if (links && links.length > 0) {
                      otherSemanticLinks.push(...links);
                    }
                  } catch (err) {
                    console.warn(`Error fetching semantic links for ${sourceTag}->${targetTag}:`, err);
                  }
                }
              }
            }
            
            // Update post with detected relationships
            if (subclassFound) {
              postCopy.hasSubclassRelation = true;
              postCopy.semanticLinks = subclassLinks;
              
              // Set primary relationship for display
              if (subclassLinks.length > 0) {
                postCopy.primaryRelationship = subclassLinks.find(link => 
                  link.isSubclassRelation && link.hops === 1
                ) || subclassLinks[0];
              }
            }
            else if (commonParentFound) {
              postCopy.hasCommonParentRelation = true;
              postCopy.commonParents = commonParentsInfo.commonParents;
            }
            else if (transitiveFound) {
              postCopy.hasTransitiveRelation = true;
              postCopy.transitiveRelations = transitiveInfo.transitiveRelations;
            }
            else if (otherSemanticLinks.length > 0) {
              postCopy.semanticLinks = otherSemanticLinks;
              
              // Set primary relationship for display
              postCopy.primaryRelationship = otherSemanticLinks.reduce((prev, current) => {
                // Prioritize by relation type and hop count
                const prevScore = prev.relationWeight / prev.hops;
                const currentScore = current.relationWeight / current.hops;
                return currentScore > prevScore ? current : prev;
              });
            }
            
            // Calculate similarity score
            const scoreResult = calculateSimilarityScore(postCopy, thread);
            postCopy.similarityScore = scoreResult.score;
            postCopy.scoreDetails = scoreResult.details;
            
            // Generate explanation
            postCopy.relationshipExplanation = generateRelationshipExplanation(postCopy, thread);
            
            return postCopy;
          } catch (err) {
            console.error("Error processing post:", err);
            return {
              ...post,
              semanticLinks: [],
              hasSubclassRelation: false,
              similarityScore: post.matchingTagCount > 0 ? 30 * post.matchingTagCount : 0
            };
          }
        })
      );
      
      // Filter results to only include relevant posts
      // Lower threshold for posts with any semantic relationship
      const relevantResults = processedResults.filter(post => 
        post.matchingTagCount > 0 ||
        post.hasSubclassRelation || 
        post.hasCommonParentRelation ||
        post.hasTransitiveRelation ||
        (post.semanticLinks && post.semanticLinks.length > 0 && post.similarityScore >= 12)
      );
      
      console.log(`Found ${relevantResults.length} related posts`);
      
      // Sort with enhanced priority system
      postsWithSemanticInfo = relevantResults.sort((a, b) => {
        // First by score - simplify sorting to just use the calculated score
        return b.similarityScore - a.similarityScore;
      });
      
    } catch (error) {
      console.error('Error in processPosts:', error);
      
      // Fallback to direct matches only
      postsWithSemanticInfo = internalSimilarPosts
        .filter(post => post.matchingTagCount > 0)
        .map(post => ({
          ...post,
          semanticLinks: [],
          hasSubclassRelation: false,
          similarityScore: 30 * (post.matchingTagCount || 0),
          relationshipExplanation: `Shares ${post.matchingTagCount} tag${post.matchingTagCount !== 1 ? 's' : ''}`
        }));
    } finally {
      loadingSemantic = false;
      loadingSubclassRelations = false;
    }
  }

  // Add this helper function to get a user-friendly relation name from a property ID
  function getFriendlyRelationName(propertyId) {
    // Strip any URL parts to get just the property ID
    const id = propertyId?.includes(':') ? propertyId.split(':').pop() : propertyId;
    
    if (!id) return 'relationship';

    // Check if it's in our relation types dictionary
    if (relationTypes[id]) {
      // Return the name in lowercase and more readable format
      const name = relationTypes[id].name.toLowerCase();
      return name.replace(/_/g, ' '); // Replace underscores with spaces
    }

    // Map common properties to friendly names for those not in relationTypes
    const commonProperties = {
      'P31': 'instance of',
      'P279': 'subclass of',
      'P171': 'parent taxon',
      'P361': 'part of',
      'P527': 'has part',
      'P1889': 'different from',
      'P460': 'same as',
      'P1382': 'coincident with',
      'P366': 'used for',
      'P1535': 'used by',
      'P1552': 'has quality',
      'P186': 'material',
      'P137': 'operator',
      'P462': 'color'
    };

    return commonProperties[id] || 'relationship';
  }

  // Generate human-readable explanation of the relationship
  function generateRelationshipExplanation(post, currentThread) {
    if (!post) return '';
    
    // List of overly generic parent classes to filter out
    const genericParentClasses = [
      "taxon", "material", "entity", "object", "thing", "item", "substance", 
      "physical object", "abstract object", "physical entity", "abstract entity",
      "chemical compound", "chemical element", "chemical substance", "product"
    ];
    
    // Helper function to check if a label is too generic
    const isTooGeneric = (label) => {
      if (!label) return true;
      return genericParentClasses.some(generic => 
        label.toLowerCase() === generic.toLowerCase() || 
        label.toLowerCase().includes(`(${generic.toLowerCase()})`)
      );
    };
    
    // Helper to clean relation labels - remove "(link)" and other artifacts
    const cleanRelationLabel = (label) => {
      if (!label) return '';
      
      // Remove "(link)" and similar parenthetical expressions
      return label.replace(/\s*\([^)]*\)/g, '').trim();
    };
    
    // Start with straightforward tag matches
    if (post.matchingTagCount > 0) {
      const matchingTags = post.tags.filter(tag => currentThread.tags.includes(tag));
      const tagLabels = matchingTags
        .slice(0, Math.min(3, matchingTags.length))
        .map(tag => $tagDetails[tag]?.label || tag)
        .filter(Boolean);
      
      if (tagLabels.length > 0) {
        let explanation = `Both posts discuss ${tagLabels.map(label => `"${label}"`).join(', ')}`;
        
        if (matchingTags.length > 3) {
          explanation += ` and ${matchingTags.length - 3} other similar topics`;
        }
        
        return explanation;
      }
    }
    
    // Check for common parent relationship - filtering out overly generic parents
    if (post.hasCommonParentRelation && post.commonParents && post.commonParents.length > 0) {
      // Try to find a non-generic parent from all available parents
      const nonGenericParent = post.commonParents.find(parent => 
        parent.label && !isTooGeneric(parent.label)
      );
      
      // If we found a non-generic parent, use it
      if (nonGenericParent) {
        return `Both are types of "${nonGenericParent.label}"`;
      }
      
      // If all parents are too generic, don't use common parent explanation
      // Fall through to other relationship types
    }
    
    // Check for subclass relationships
    if (post.hasSubclassRelation && post.primaryRelationship) {
      if (post.primaryRelationship.hierarchyType === 'child') {
        if ($tagDetails[post.primaryRelationship.sourceEntity] && $tagDetails[post.primaryRelationship.targetEntity]) {
          const sourceLabel = $tagDetails[post.primaryRelationship.sourceEntity].label;
          const targetLabel = $tagDetails[post.primaryRelationship.targetEntity].label;
          
          // Skip if either label is too generic
          if (!isTooGeneric(sourceLabel) && !isTooGeneric(targetLabel)) {
            return `"${sourceLabel}" is a subclass of "${targetLabel}"`;
          }
        }
      } else if (post.primaryRelationship.hierarchyType === 'parent') {
        if ($tagDetails[post.primaryRelationship.targetEntity] && $tagDetails[post.primaryRelationship.sourceEntity]) {
          const sourceLabel = $tagDetails[post.primaryRelationship.sourceEntity].label;
          const targetLabel = $tagDetails[post.primaryRelationship.targetEntity].label;
          
          // Skip if either label is too generic
          if (!isTooGeneric(targetLabel) && !isTooGeneric(sourceLabel)) {
            return `"${targetLabel}" is a subclass of "${sourceLabel}"`;
          }
        }
      }
      
      // If we get here, the subclass relationship was too generic
      // Fall through to other options
    }
    
    // Check for transitive relationships - improved to use friendly relation names
    if (post.hasTransitiveRelation && post.transitiveRelations && post.transitiveRelations.length > 0) {
      const relation = post.transitiveRelations[0];
      
      // First try to use the entity label if it's not too generic
      let intermediateLabel = relation.intermediateEntityLabel || '';
      intermediateLabel = intermediateLabel.replace(/Q\d+\s*(\([^)]*\))?/g, '').trim();
      intermediateLabel = cleanRelationLabel(intermediateLabel);
      
      // Extract the property ID from the relationId
      const relationId = relation.relationId;
      const friendlyRelationName = getFriendlyRelationName(relationId);
      
      // Skip if the intermediate label is not too generic
      if (intermediateLabel && !isTooGeneric(intermediateLabel)) {
        return `Connected through ${intermediateLabel}`;
      }
      
      // Use the friendly relation name if available
      if (friendlyRelationName !== 'relationship') {
        return `Connected through ${friendlyRelationName} relationship`;
      }
      
      // If relation label is available and not too generic
      const relationLabel = cleanRelationLabel(relation.relationLabel);
      if (relationLabel && !isTooGeneric(relationLabel)) {
        return `Connected through ${relationLabel.toLowerCase()}`;
      }
    }
    
    // Check for any other semantic relationships
    if (post.semanticLinks && post.semanticLinks.length > 0) {
      const link = post.semanticLinks[0];
      
      // Try to use entity labels if available
      if ($tagDetails[link.sourceEntity] && $tagDetails[link.targetEntity]) {
        const sourceLabel = $tagDetails[link.sourceEntity].label;
        const targetLabel = $tagDetails[link.targetEntity].label;
        
        // Skip if either label is too generic
        if (!isTooGeneric(sourceLabel) && !isTooGeneric(targetLabel)) {
          // Use the friendly relation name if available
          const friendlyRelation = getFriendlyRelationName(link.relationId) || link.relationLabel?.toLowerCase() || 'related to';
          return `"${sourceLabel}" is ${friendlyRelation} "${targetLabel}"`;
        }
      }
      
      // If source and target are not suitable, use the relation type
      const friendlyRelation = getFriendlyRelationName(link.relationId);
      if (friendlyRelation !== 'relationship') {
        return `Topics connected by ${friendlyRelation} relationship`;
      }
      
      // Try the relation label as fallback
      const relationLabel = cleanRelationLabel(link.relationLabel);
      if (relationLabel && !isTooGeneric(relationLabel)) {
        return `Topics have a ${relationLabel.toLowerCase()} connection`;
      }
    }
    
    // More specific fallback based on similarity score
    if (post.similarityScore > 50) {
      return "Topics are closely connected in the knowledge graph";
    } else if (post.similarityScore > 30) {
      return "Topics share knowledge graph connections";
    } else {
      // Always provide some context for the relationship
      if (post.semanticLinks && post.semanticLinks.length > 0) {
        // Try to find the most descriptive relation type
        const mostSignificantLink = post.semanticLinks.reduce((prev, curr) => {
          const prevWeight = prev.relationWeight || 0.5;
          const currWeight = curr.relationWeight || 0.5;
          return currWeight > prevWeight ? curr : prev;
        });
        
        const friendlyRelation = getFriendlyRelationName(mostSignificantLink.relationId);
        if (friendlyRelation !== 'relationship') {
          return `Topics share a ${friendlyRelation} connection`;
        }
        
        // Default to a more informative generic statement
        return "Topics are connected through semantic relationships";
      }
      return "Topics have conceptual similarities";
    }
  }

  // Track thread loading state to handle page refreshes properly
  let threadLoaded = false;

  // Enhanced reactive watcher for thread changes
  $: {
    // This will run both when navigating between threads AND when thread data becomes available after refresh
    if (thread?.id) {
      if (!threadLoaded || previousThreadId !== thread.id) {
        console.log(`Thread data loaded or changed: ${thread.id}`);
        previousThreadId = thread.id;
        threadLoaded = true;
        resetComponent();
      }
    } else {
      // Reset state when thread becomes unavailable
      threadLoaded = false;
      internalSimilarPosts = [];
      postsWithSemanticInfo = [];
      loadingSemantic = false;
    }
  }

  // Watch for store changes that might affect this component
  $: if (threadLoaded && thread?.id && $threadStore && $threadStore.length > 0) {
    // Thread store may have been refreshed with new data
    const updatedThread = $threadStore.find(t => String(t.id) === String(thread.id));
    if (updatedThread && updatedThread.tags?.length > 0) {
      // Check if tags have changed
      const currentTags = thread.tags || [];
      const newTags = updatedThread.tags || [];
      
      if (JSON.stringify(currentTags) !== JSON.stringify(newTags)) {
        console.log("Thread tags have changed, updating similar posts");
        initializeSimilarPosts();
      }
    }
  }

  // Reset component when navigating to a new thread
  function resetComponent() {
    console.log("Resetting similar posts component - new thread or refresh detected");
    internalSimilarPosts = [];
    postsWithSemanticInfo = [];
    loadingSemantic = true;
    loadingSubclassRelations = false;
    currentSlide = 0;
    
    // Limit cache sizes to prevent memory buildup during navigation
    if (subclassCache.size > 100) subclassCache = new Map();
    if (commonParentCache.size > 100) commonParentCache = new Map();
    if (relationshipExplanations.size > 100) relationshipExplanations = new Map();
    
    // Initialize the component for the new thread
    if (thread && thread.id && thread.tags && $threadStore) {
      initializeSimilarPosts();
    }
  }

  // Add the missing function definition here
  // Initialize similar posts for the current thread
  function initializeSimilarPosts() {
    if (!thread || !thread.id || !thread.tags || !$threadStore) {
      internalSimilarPosts = [];
      loadingSemantic = false;
      return;
    }
    
    // Find all posts (not just direct tag matches)
    internalSimilarPosts = $threadStore.filter(t => {
      // Don't include the current thread
      if (String(t.id) === String(thread.id)) return false;

      // Check if there are any tags at all (both posts must have tags)
      if (!thread.tags || !t.tags || thread.tags.length === 0 || t.tags.length === 0) return false;

      // Include all posts with tags for further semantic analysis
      return true;
    }).map(post => {
      // Calculate number of matching tags
      const matchingTagCount = post.tags.filter(tag => thread?.tags?.includes(tag)).length;
      return { ...post, matchingTagCount };
    });

    // Sort direct matches first, then by recency for potential semantic matches
    const directMatches = internalSimilarPosts.filter(post => post.matchingTagCount > 0);
    const potentialSemanticMatches = internalSimilarPosts.filter(post => post.matchingTagCount === 0);
    
    directMatches.sort((a, b) => b.matchingTagCount - a.matchingTagCount);
    potentialSemanticMatches.sort((a, b) => b.id - a.id); // Most recent first
    
    internalSimilarPosts = [...directMatches, ...potentialSemanticMatches.slice(0, 30)]; // Take up to 30 potential semantic matches
    
    // Trigger similarity processing if there are posts to process
    if (internalSimilarPosts.length > 0) {
      processPosts();
    } else {
      loadingSemantic = false;
    }
  }

  // More robust initialization on mount with a short delay to handle timing issues
  onMount(() => {
    // Short delay to ensure that thread data has time to load after page refresh
    setTimeout(() => {
      if (thread?.id && thread.tags && thread.tags.length > 0) {
        console.log("Initializing similar posts on mount");
        previousThreadId = thread.id;
        threadLoaded = true;
        resetComponent();
      } else {
        loadingSemantic = false;
      }
    }, 300); // 300ms delay to allow page data to load

    // Set up event listener for window focus to refresh data when returning to the page
    const handleFocus = () => {
      if (threadLoaded && thread?.id) {
        console.log("Window focused, checking for updates to similar posts");
        initializeSimilarPosts();
      }
    };
    
    window.addEventListener('focus', handleFocus);
    
    return () => {
      window.removeEventListener('focus', handleFocus);
    };
  });

  // Clean up on destroy
  onDestroy(() => {
    // Clear large caches to free memory
    subclassCache.clear();
    commonParentCache.clear();
    relationshipExplanations.clear();
  });
  
  // Execute after each UI update to adjust navigation state
  afterUpdate(() => {
    // Fix for incorrect carousel page after route change
    if (postsWithSemanticInfo.length > 0 && currentSlide > 0) {
      const maxSlide = Math.max(0, postsWithSemanticInfo.length - visibleSlides);
      if (currentSlide > maxSlide) {
        currentSlide = maxSlide;
      }
    }
  });

  // Update similar posts reference when data changes
  $: similarPosts = postsWithSemanticInfo;

  // Calculate total pages for pagination
  $: totalPages = similarPosts ? Math.ceil(similarPosts.length / visibleSlides) : 0;

  // Update visible slides when container width changes
  $: if (containerWidth) {
    updateVisibleSlides();
  }
  
</script>

{#if internalSimilarPosts.length > 0}
  <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
    <div class="p-2 border-b border-neutral-100 dark:border-neutral-800">
      <div>
        <h3 class="text-sm font-medium text-neutral-900 dark:text-white flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5 text-teal-600" viewBox="0 0 20 20" fill="currentColor">
            <path d="M7 3a1 1 0 000 2h6a1 1 0 100-2H7zM4 7a1 1 0 011-1h10a1 1 0 110 2H5a1 1 0 01-1-1zM2 11a2 2 0 012-2h12a2 2 0 012 2v4a2 2 0 01-2 2H4a2 2 0 01-2-2v-4z" />
          </svg>
          Similar Posts ({postsWithSemanticInfo.length || internalSimilarPosts.length})
        </h3>
        <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
          Based on {thread?.tags?.length || 0} tag{thread?.tags?.length !== 1 ? 's' : ''}
          {#if loadingSubclassRelations}
            <span class="italic ml-1">(calculating relations...)</span>
          {/if}
        </p>
      </div>
    </div>

    <!-- Carousel container with proper binding -->
    <div
      class="relative overflow-hidden"
      bind:this={carouselContainer}
      bind:clientWidth={containerWidth}
    >
      <div
        class="flex py-2 px-1.5"
        on:touchstart={handleTouchStart}
        on:touchmove={handleTouchMove}
        on:touchend={handleTouchEnd}
      >
        {#if loadingSemantic}
          <!-- Loading state - explicitly included instead of using comment -->
          <div class="flex-1 flex items-center justify-center py-6">
            <div class="flex flex-col items-center">
              <svg class="animate-spin h-5 w-5 text-teal-500 mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span class="text-sm text-neutral-500 dark:text-neutral-400">
                {#if loadingSubclassRelations}
                  Finding related posts...
                {:else}
                  Looking for similar posts...
                {/if}
              </span>
            </div>
          </div>
        {:else if postsWithSemanticInfo.length > 0}
          <div
            class="flex transition-transform duration-300 ease-in-out will-change-transform"
            style="transform: translateX(-{currentSlide * (100 / visibleSlides)}%); width: {postsWithSemanticInfo.length * (100 / visibleSlides)}%"
          >
            {#each postsWithSemanticInfo as post, index (post.id)}
              <!-- Only render posts that would be visible or close to becoming visible -->
              {#if index >= Math.max(0, currentSlide - 1) && index < currentSlide + visibleSlides + 1}
                <div class="px-1.5 snap-center" style="width: {100 / visibleSlides}%"> 
                  <a href={`/thread/${post.id}`} class="block h-full">
                    <!-- Post card with optimized compact layout -->
                    <div class="bg-neutral-50 dark:bg-neutral-900 rounded border border-neutral-200 dark:border-neutral-800 shadow-sm h-full hover:border-neutral-300 dark:hover:border-neutral-700 transition-all hover:shadow-md overflow-hidden flex flex-col">
                      <!-- Image container -->
                      <div class="aspect-video w-full overflow-hidden relative bg-neutral-100 dark:bg-neutral-850 border-b border-neutral-200 dark:border-neutral-800">
                        {#if post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0)}
                          <img 
                            src={post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0 ? post.mediaFiles[0].url : '')} 
                            alt={post.title} 
                            class="w-full h-full object-cover"
                            loading="lazy"
                          />
                        {:else}
                          <div class="absolute inset-0 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-300 dark:text-neutral-700" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                            </svg>
                          </div>
                        {/if}
                        
                        <!-- Match percentage display -->
                        <div class="absolute bottom-0 left-0 right-0 flex justify-between items-center px-2 py-1 bg-gradient-to-t from-black/70 to-transparent">
                          <div class="px-1.5 py-0.5 bg-black/40 backdrop-blur-sm rounded-sm text-white text-xs font-medium">
                            {post.similarityScore}% match
                          </div>
                        </div>
                      </div>
                      
                      <!-- Content section with optimized spacing -->
                      <div class="p-2 flex-1 flex flex-col">
                        <!-- Post title - single line only -->
                        <h4 class="font-medium text-sm line-clamp-1 text-neutral-900 dark:text-white mb-1.5">
                          {post.title}
                        </h4>
                        
                        <!-- Enhanced relationship explanation -->
                        <div class="text-xs text-neutral-600 dark:text-neutral-300 border-l-2 border-teal-500 dark:border-teal-600 pl-2 py-0.5 mb-auto bg-teal-50/50 dark:bg-teal-900/10 rounded-r">
                          {#if post.relationshipExplanation}
                            {post.relationshipExplanation}
                          {:else}
                            Topics are semantically related
                          {/if}
                        </div>
                        
                        <!-- Author and info footer - no extra margin -->
                        <div class="mt-1.5 pt-1.5 border-t border-neutral-100 dark:border-neutral-800 text-xs text-neutral-500 dark:text-neutral-400 flex items-center justify-between">
                          <div class="truncate max-w-[80px]">{post.author}</div>
                          <div class="flex items-center gap-2">
                            <div class="flex items-center">
                              <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                              </svg>
                              {post.commentCount || 0}
                            </div>
                            
                            <!-- Both resolved and unresolved status indicators -->
                            {#if post.solved}
                              <div class="flex items-center text-emerald-600">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                                </svg>
                              </div>
                            {:else}
                              <div class="flex items-center text-rose-800">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                                </svg>
                              </div>
                            {/if}
                          </div>
                        </div>
                      </div>
                    </div>
                  </a>
                </div>
              {:else}
                <!-- Placeholder for non-visible slides to maintain correct positioning -->
                <div class="px-1.5 snap-center" style="width: {100 / visibleSlides}%"></div>
              {/if}
            {/each}
          </div>
        {:else}
          <!-- No posts found message -->
          <div class="flex-1 flex items-center justify-center py-6">
            <div class="flex flex-col items-center">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-neutral-300 dark:text-neutral-700 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span class="text-sm text-neutral-500 dark:text-neutral-400">
                No similar posts found
              </span>
            </div>
          </div>
        {/if}
      </div>

      <!-- Fixed pagination indicators with more robust approach -->
      {#if similarPosts && similarPosts.length > visibleSlides}
        <div class="flex justify-center space-x-1 pb-2 mt-1">
          {#each Array(pagination.totalPages) as _, i}
            <button
              class="w-2 h-2 rounded-full transition-all focus:outline-none focus:ring-1 focus:ring-teal-500 {i === pagination.currentPage ? 'bg-teal-500 scale-125' : 'bg-neutral-300 dark:bg-neutral-700 hover:bg-neutral-400 dark:hover:bg-neutral-600'}"
              on:click={() => goToPage(i)}
              aria-label={`Go to slide group ${i + 1}`}
            ></button>
          {/each}
        </div>
      {/if}
      
      <!-- Improved overlay navigation buttons tied to pagination -->
      {#if similarPosts && similarPosts.length > visibleSlides}
        <div class="block">
          <button 
            class="absolute left-1 top-1/2 transform -translate-y-1/2 w-8 h-8 flex items-center justify-center rounded-full bg-white/80 dark:bg-black/50 shadow hover:bg-white dark:hover:bg-black/70 {!pagination.hasPrev ? 'opacity-0 pointer-events-none' : 'opacity-100'} transition-opacity focus:outline-none focus:ring-2 focus:ring-teal-500"
            on:click={prevSlide}
            disabled={!pagination.hasPrev}
            aria-label="Previous slide group"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button 
            class="absolute right-1 top-1/2 transform -translate-y-1/2 w-8 h-8 flex items-center justify-center rounded-full bg-white/80 dark:bg-black/50 shadow hover:bg-white dark:hover:bg-black/70 {!pagination.hasNext ? 'opacity-0 pointer-events-none' : 'opacity-100'} transition-opacity focus:outline-none focus:ring-2 focus:ring-teal-500"
            on:click={nextSlide}
            disabled={!pagination.hasNext}
            aria-label="Next slide group"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      {/if}
    </div>
  </div>
{:else if !loadingSemantic}
  <!-- No similar posts found state -->
  <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden p-2.5">
    <div class="flex items-center gap-2.5">
      <div class="w-10 h-10 rounded-full bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center shrink-0">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <div>
        <h3 class="text-sm font-medium text-neutral-900 dark:text-white">No similar posts found</h3>
        {#if thread?.tags?.length > 0}
          <p class="text-xs text-neutral-500 dark:text-neutral-400">We couldn't find any other posts with matching or related tags.</p>
        {:else}
          <p class="text-xs text-neutral-500 dark:text-neutral-400">Add tags to this post to discover similar content.</p>
        {/if}
      </div>
    </div>
  </div>
{:else}
  <!-- Loading state - separate component -->
  <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden p-4">
    <div class="flex justify-center">
      <div class="flex flex-col items-center">
        <svg class="animate-spin h-6 w-6 text-teal-500 mb-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="text-sm text-neutral-500">Looking for similar posts...</p>
      </div>
    </div>
  </div>
{/if}
