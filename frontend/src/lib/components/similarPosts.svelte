<script>
  import { writable } from 'svelte/store';
  import { onMount, onDestroy } from 'svelte';
  import { threadStore } from '../../threadStore'; // Import threadStore
  import { PUBLIC_API_URL } from "$env/static/public"; // Needed for tag fetching

  // Props for the carousel
  export let thread = {}; // Current thread object
  export let tagDetails = writable({}); // Accept the store itself

  // Internal state for calculated similar posts
  let internalSimilarPosts = [];

  // Current slide index for carousel
  // ... existing carousel state ...
  let currentSlide = 0;
  let touchStartX = 0;
  let touchEndX = 0;
  let visibleSlides = 3;
  let containerWidth;

  // New loading state for semantic relationships
  // ... existing semantic state ...
  let loadingSemantic = true;
  let postsWithSemanticInfo = [];

  // Domain-agnostic semantic linking ruleset with hierarchical relationship types
  const relationTypes = {
    // Hierarchical relationships
    'P31': { name: 'INSTANCE_OF', weight: 0.8, hierarchy: 'child' },
    'P279': { name: 'SUBCLASS_OF', weight: 0.9, hierarchy: 'child' },
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

  // Implementation of semantic similarity scoring with hierarchical bonuses
  function calculateSimilarityScore(post, currentThread) {
    // Base score from matching tag count
    let score = 30 * (post.matchingTagCount || 0);
    
    // Add points for each semantic relationship found
    if (post.semanticLinks && post.semanticLinks.length > 0) {
      post.semanticLinks.forEach(link => {
        const relationWeight = link.relationWeight || 0.5;
        const hopPenalty = 0.7 ** (link.hops - 1); // Penalty for indirect links
        
        // Add hierarchical relationship bonuses
        let hierarchyBonus = 1.0;
        if (link.hierarchyType) {
          switch(link.hierarchyType) {
            case 'parent': hierarchyBonus = 1.2; break;
            case 'child': hierarchyBonus = 1.1; break;
            case 'sibling': hierarchyBonus = 1.3; break; // Siblings are highly relevant
            case 'cousin': hierarchyBonus = 0.9; break;
            case 'uncle': hierarchyBonus = 0.85; break;
            default: hierarchyBonus = 1.0;
          }
        }
        
        score += 25 * relationWeight * hopPenalty * hierarchyBonus;
      });
    }
    
    // Normalize score to 0-100 range and round to nearest integer
    return Math.min(100, Math.round(score));
  }

  // Enhanced query to find semantic connections with hierarchical relationships
  async function fetchSemanticLinks(sourceEntityId, targetEntityId) {
    try {
      // Enhanced SPARQL query to find hierarchical relationships
      const sparqlQuery = `
        SELECT ?source ?target ?relation ?relationLabel ?hierarchy ?hop1 ?hop1Label ?hop2 ?hop2Label WHERE {
          VALUES ?source { wd:${sourceEntityId} }
          VALUES ?target { wd:${targetEntityId} }
          
          # Direct relationship (1 hop)
          {
            ?source ?relation ?target .
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
          UNION
          # 2-hop path with hierarchy identification
          {
            ?source ?relation ?hop1 .
            ?hop1 ?relation2 ?target .
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
          UNION
          # 3-hop path
          {
            ?source ?relation ?hop1 .
            ?hop1 ?relation2 ?hop2 .
            ?hop2 ?relation3 ?target .
            BIND(3 AS ?hops)
          }
          
          SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
        LIMIT 5
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3000); // 3-second timeout
      
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
        
        return {
          sourceEntity: sourceEntityId,
          targetEntity: targetEntityId,
          relationType: relInfo.name,
          relationWeight: relInfo.weight,
          relationLabel: result.relationLabel?.value || 'related to',
          hops: result.hops?.value ? parseInt(result.hops.value) : 1,
          hierarchyType: hierarchyType,
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
      console.error('Error fetching semantic links:', error);
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

  // Reactive calculation for similar posts
  $: {
    if (thread && thread.id && thread.tags && $threadStore) {
      internalSimilarPosts = $threadStore.filter(t => {
        // Don't include the current thread
        if (String(t.id) === String(thread.id)) return false;

        // Check if there are any matching tags
        if (!thread.tags || !t.tags) return false;

        // Count matching tags
        const matchingTags = t.tags.filter(tag => thread.tags.includes(tag));

        // Only include posts with 1 or more matching tags
        return matchingTags.length > 0;
      }).map(post => {
        // Calculate number of matching tags for sorting
        const matchingTagCount = post.tags.filter(tag => thread?.tags?.includes(tag)).length;
        return { ...post, matchingTagCount };
      }).sort((a, b) => {
        // Sort by number of matching tags (descending)
        return b.matchingTagCount - a.matchingTagCount;
      }).slice(0, 10); // Limit to 10 similar posts

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

  // Update visible slides based on container width
  $: {
    if (containerWidth) {
      if (containerWidth < 640) visibleSlides = 1;
      else if (containerWidth < 1024) visibleSlides = 2;
      else visibleSlides = 3;
    }
  }

  // Functions to navigate carousel
  function nextSlide() {
    if (currentSlide < similarPosts.length - visibleSlides) {
      currentSlide++;
    }
  }

  function prevSlide() {
    if (currentSlide > 0) {
      currentSlide--;
    }
  }

  // Touch handlers for mobile swipe
  function handleTouchStart(e) {
    touchStartX = e.touches[0].clientX;
  }

  function handleTouchEnd(e) {
    touchEndX = e.changedTouches[0].clientX;
    handleSwipe();
  }

  function handleSwipe() {
    const swipeDistance = touchEndX - touchStartX;
    if (swipeDistance > 50) {
      // Swipe right
      prevSlide();
    } else if (swipeDistance < -50) {
      // Swipe left
      nextSlide();
    }
  }

  // Function to get a friendly display for relation types
  function getRelationDisplay(relationType) {
    switch(relationType) {
      case 'SUBCLASS_OF': return 'BROADER';
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
          icon: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
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

  // Process posts to add semantic information (operates on internalSimilarPosts)
  async function processPosts() {
    loadingSemantic = true;

    try {
      const results = await Promise.all(
        internalSimilarPosts.map(async (post) => { // Use internalSimilarPosts
          const postCopy = { ...post };

          // Find shared tags between this post and the current thread
          const sharedTags = post.tags.filter(tag => thread.tags.includes(tag));

          if (sharedTags.length > 0) {
            // Get semantic links for each pair of tags
            const semanticLinks = [];
            
            // Limit to first 2 tags for performance
            const limitedSharedTags = sharedTags.slice(0, 2);
            
            for (const sourceTag of limitedSharedTags) {
              // Find semantically related tags in the other post
              const otherTags = post.tags.filter(t => !thread.tags.includes(t)).slice(0, 2);
              
              for (const targetTag of otherTags) {
                const links = await fetchSemanticLinks(sourceTag, targetTag);
                if (links.length > 0) {
                  semanticLinks.push(...links);
                }
              }
            }
            
            // Find most interesting/close relationship 
            postCopy.primaryRelationship = semanticLinks.length > 0 
              ? semanticLinks.reduce((prev, current) => {
                  // Prioritize hierarchical relationships, then by weight
                  const prevIsHierarchy = ['parent', 'child', 'sibling', 'cousin'].includes(prev.hierarchyType);
                  const currentIsHierarchy = ['parent', 'child', 'sibling', 'cousin'].includes(current.hierarchyType);
                  
                  if (currentIsHierarchy && !prevIsHierarchy) return current;
                  if (!currentIsHierarchy && prevIsHierarchy) return prev;
                  
                  // Both or neither are hierarchical, compare by weight and hops
                  const prevScore = prev.relationWeight / prev.hops;
                  const currentScore = current.relationWeight / current.hops;
                  
                  return currentScore > prevScore ? current : prev;
                })
              : null;
            
            postCopy.semanticLinks = semanticLinks;
            
            // Calculate similarity score
            postCopy.similarityScore = calculateSimilarityScore(postCopy, thread);
          } else {
            postCopy.semanticLinks = [];
            postCopy.similarityScore = 30 * (post.matchingTagCount || 0);
          }
          
          return postCopy;
        })
      );

      // Sort results by semantic relationship quality and similarity score
      postsWithSemanticInfo = results.sort((a, b) => {
        // First check for primary relationship
        const aHasHierarchy = a.primaryRelationship && ['parent', 'child', 'sibling', 'cousin'].includes(a.primaryRelationship.hierarchyType);
        const bHasHierarchy = b.primaryRelationship && ['parent', 'child', 'sibling', 'cousin'].includes(b.primaryRelationship.hierarchyType);
        
        if (aHasHierarchy && !bHasHierarchy) return -1;
        if (!aHasHierarchy && bHasHierarchy) return 1;
        
        // Then check similarity score
        return b.similarityScore - a.similarityScore;
      });
    } catch (error) {
      console.error('Error processing semantic links:', error);
      // Fallback using internalSimilarPosts
      postsWithSemanticInfo = internalSimilarPosts.map(post => ({
        ...post,
        semanticLinks: [],
        similarityScore: 30 * (post.matchingTagCount || 0)
      }));
    } finally {
      loadingSemantic = false;
    }
  }

  onMount(() => {
    // Initial processing if posts are already available
    if (internalSimilarPosts && internalSimilarPosts.length > 0) {
      processPosts();
    } else {
      loadingSemantic = false; // Ensure loading is false if no posts initially
    }
  });
</script>

{#if internalSimilarPosts.length > 0}
  <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 overflow-hidden">
    <div class="p-2.5 border-b border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
      <div>
        <h3 class="text-sm font-medium text-neutral-900 dark:text-white flex items-center">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1.5 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          Similar Posts ({internalSimilarPosts.length})
        </h3>
        <p class="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
          Based on {thread?.tags?.length || 0} tag{thread?.tags?.length !== 1 ? 's' : ''}
        </p>
      </div>

      <!-- Navigation controls in header -->
      <div class="flex items-center gap-2">
        <button
          class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
          on:click={prevSlide}
          disabled={currentSlide === 0}
          aria-label="Previous slide"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <button
          class="w-7 h-7 flex items-center justify-center rounded-full bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 disabled:opacity-40 disabled:cursor-not-allowed"
          on:click={nextSlide}
          disabled={currentSlide >= internalSimilarPosts.length - visibleSlides}
          aria-label="Next slide"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>
    </div>

    <div
      class="relative overflow-hidden"
      bind:clientWidth={containerWidth}
    >
      <div
        class="flex py-2.5 px-1"
        on:touchstart={handleTouchStart}
        on:touchend={handleTouchEnd}
      >
        <div
          class="flex transition-transform duration-300 ease-in-out"
          style="transform: translateX(-{currentSlide * (100 / visibleSlides)}%); width: {internalSimilarPosts.length * (100 / visibleSlides)}%"
        >
          {#if loadingSemantic}
            <!-- Skeleton loaders for posts -->
            {#each Array(Math.min(3, internalSimilarPosts.length)) as _, i}
              <div class="px-1.5" style="width: {100 / internalSimilarPosts.length}%">
                <div class="bg-neutral-50 dark:bg-neutral-900 rounded border border-neutral-200 dark:border-neutral-800 shadow-sm h-full animate-pulse">
                  <!-- Skeleton image -->
                  <div class="aspect-[3/2] bg-neutral-200 dark:bg-neutral-700"></div>
                  
                  <div class="p-2">
                    <!-- Skeleton title -->
                    <div class="h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-3/4 mb-2"></div>
                    
                    <!-- Skeleton tags -->
                    <div class="flex items-center gap-1 mb-2">
                      <div class="h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-10"></div>
                      <div class="h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-14"></div>
                    </div>
                    
                    <!-- Skeleton semantic links -->
                    <div class="h-10 bg-neutral-200 dark:bg-neutral-700 rounded mb-2"></div>
                    
                    <!-- Skeleton meta info -->
                    <div class="flex justify-between items-center pt-1 border-t border-neutral-100 dark:border-neutral-800">
                      <div class="h-2 bg-neutral-200 dark:bg-neutral-700 rounded w-1/3"></div>
                      <div class="h-2 bg-neutral-200 dark:bg-neutral-700 rounded w-1/4"></div>
                    </div>
                  </div>
                </div>
              </div>
            {/each}
          {:else}
            {#each postsWithSemanticInfo as post (post.id)}
              <div class="px-1.5" style="width: {100 / internalSimilarPosts.length}%"> 
                <a href={`/thread/${post.id}`} class="block h-full">
                  <div class="bg-neutral-50 dark:bg-neutral-900 rounded border border-neutral-200 dark:border-neutral-800 shadow-sm h-full hover:border-neutral-300 dark:hover:border-neutral-700 transition-all">
                    <!-- Compact image container -->
                    {#if post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0)}
                      <div class="aspect-[3/2] overflow-hidden border-b border-neutral-200 dark:border-neutral-800 bg-neutral-100 dark:bg-neutral-800">
                        <img 
                          src={post.mysteryObjectImageUrl || (post.mediaFiles && post.mediaFiles.length > 0 ? post.mediaFiles[0].url : '')} 
                          alt={post.title} 
                          class="w-full h-full object-cover"
                        />
                      </div>
                    {:else}
                      <div class="h-3"></div>
                    {/if}
                    
                    <div class="p-2">
                      <!-- Post title with primary relationship badge when available -->
                      <div class="flex justify-between items-start mb-1 gap-1">
                        <h4 class="font-medium text-xs line-clamp-1 flex-1">{post.title}</h4>
                        {#if post.primaryRelationship && post.primaryRelationship.hierarchyType}
                          {@const hierarchyStyle = getHierarchyStyle(post.primaryRelationship.hierarchyType)}
                          <div class="shrink-0 flex items-center rounded px-1 py-0.5 bg-neutral-100 dark:bg-neutral-800 text-[8px] whitespace-nowrap" title="{getHierarchyDisplay(post.primaryRelationship.hierarchyType)} relationship">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5 {hierarchyStyle.color}" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={hierarchyStyle.icon} />
                            </svg>
                            <span class="truncate max-w-[40px]">{getHierarchyDisplay(post.primaryRelationship.hierarchyType)}</span>
                          </div>
                        {/if}
                      </div>
                      
                      <!-- Tags and similarity score -->
                      <div class="flex items-center gap-1 mb-1 overflow-x-auto scrollbar-none">
                        <!-- Similarity score badge -->
                        <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-teal-100 dark:bg-teal-900/30 text-teal-800 dark:text-teal-300 border border-teal-200 dark:border-teal-800/50 flex items-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                          <span>{post.similarityScore || Math.round(30 * post.matchingTagCount)}</span>
                        </span>

                        <!-- Matching tag count -->
                        <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 border border-blue-200 dark:border-blue-800/50 flex items-center">
                          <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                            <path fill-rule="evenodd" d="M17.707 9.293a1 1 0 010 1.414l-7 7a1 1 0 01-1.414 0l-7-7A.997.997 0 012 10V5a3 3 0 013-3h5c.256 0 .512.098.707.293l7 7zM5 6a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                          </svg>
                          <span>{post.matchingTagCount}</span>
                        </span>

                        {#each post.tags.filter(tag => thread?.tags?.includes(tag)).slice(0, 1) as tag}
                          {#if $tagDetails[tag]}
                            <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700 truncate max-w-[80px]">
                              {$tagDetails[tag].label}
                            </span>
                          {:else}
                             <!-- Show placeholder if tag detail not yet loaded -->
                             <span class="shrink-0 px-1.5 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-neutral-700 animate-pulse">...</span>
                          {/if}
                        {/each}

                        {#if post.tags.filter(tag => thread?.tags?.includes(tag)).length > 1}
                          <span class="shrink-0 px-1 py-0.5 rounded-full text-[10px] bg-neutral-100 dark:bg-neutral-800 text-neutral-700 dark:text-neutral-300">
                            +{post.tags.filter(tag => thread?.tags?.includes(tag)).length - 1}
                          </span>
                        {/if}
                      </div>

                      <!-- NEW: Enhanced Semantic Relationships with Hierarchy Display -->
                      {#if post.semanticLinks && post.semanticLinks.length > 0}
                        <div class="mb-1 mt-1.5 border-t border-neutral-100 dark:border-neutral-800 pt-1.5">
                          <div class="flex flex-wrap gap-1">
                            {#each post.semanticLinks.slice(0, 2) as link}
                              <div class="w-full text-[9px] flex items-center">
                                <!-- Source and target with relation and hierarchy type -->
                                <div class="flex items-center gap-1 min-w-0">
                                  <!-- Relation type badge -->
                                  <span class={`shrink-0 px-1 py-0.25 rounded text-[8px] ${getRelationColor(link.relationType)}`}>
                                    {getRelationDisplay(link.relationType)}
                                    {#if link.hops > 1}
                                      <span class="opacity-75">{link.hops}★</span>
                                    {/if}
                                  </span>
                                  
                                  <!-- Hierarchy indicator -->
                                  {#if link.hierarchyType}
                                    {@const hierarchyStyle = getHierarchyStyle(link.hierarchyType)}
                                    <span class={`inline-flex items-center ${hierarchyStyle.color}`}>
                                      <svg xmlns="http://www.w3.org/2000/svg" class="h-2.5 w-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={hierarchyStyle.icon} />
                                      </svg>
                                    </span>
                                  {/if}
                                  
                                  <!-- Entity labels -->
                                  {#if $tagDetails[link.sourceEntity] && $tagDetails[link.targetEntity]}
                                    <span class="truncate">
                                      {$tagDetails[link.sourceEntity].label} → {$tagDetails[link.targetEntity].label}
                                    </span>
                                  {:else}
                                    <!-- Show placeholder if tag details not yet loaded -->
                                    <span class="truncate animate-pulse">Loading relation...</span>
                                  {/if}
                                </div>
                              </div>
                            {/each}
                            <!-- ... -->
                          </div>
                        </div>
                      {/if}

                      <!-- Compact meta info -->
                      <div class="flex justify-between items-center text-[10px] text-neutral-500 dark:text-neutral-400 pt-1 border-t border-neutral-100 dark:border-neutral-800">
                        <div class="truncate">{post.author}</div>
                        <div class="flex items-center gap-1.5">
                          {#if post.solved}
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 text-emerald-600 dark:text-emerald-500" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
                            </svg>
                          {/if}
                          <div class="flex items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3 mr-0.5" viewBox="0 0 20 20" fill="currentColor">
                              <path fill-rule="evenodd" d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2zM7 8H5v2h2V8zm2 0h2v2H9V8zm6 0h-2v2h2V8z" clip-rule="evenodd" />
                            </svg>
                            {post.commentCount || 0}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </a>
              </div>
            {/each}
          {/if}
        </div>
      </div>

      <!-- Pagination dots for mobile -->
      {#if internalSimilarPosts.length > visibleSlides} 
        <div class="flex justify-center pb-1.5 gap-1 sm:hidden">
          {#each Array(Math.ceil(internalSimilarPosts.length / visibleSlides)) as _, i}
            <button
              class="w-1.5 h-1.5 rounded-full transition-colors {i === Math.floor(currentSlide / visibleSlides) ? 'bg-teal-600' : 'bg-neutral-300 dark:bg-neutral-700'}"
              on:click={() => currentSlide = i * visibleSlides}
              aria-label={`Go to slide ${i + 1}`}
            ></button>
          {/each}
        </div>
      {/if}
    </div>
  </div>
{:else if !loadingSemantic}
  <!-- No similar posts found -->
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
          <p class="text-xs text-neutral-500 dark:text-neutral-400">We couldn't find any other posts with matching tags.</p>
        {:else}
          <p class="text-xs text-neutral-500 dark:text-neutral-400">Add tags to this post to discover similar content.</p>
        {/if}
      </div>
    </div>
  </div>
{/if}
